/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.sqlite;

import static java.lang.String.format;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.geogit.api.ObjectId;
import org.geogit.api.Platform;
import org.geogit.api.RevObject;
import org.geogit.storage.BulkOpListener;
import org.geogit.storage.ConfigDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteDataSource;

import com.google.common.collect.Iterators;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;

import static org.geogit.storage.sqlite.Xerial.log;

/**
 * Object database based on Xerial SQLite jdbc driver.
 *
 * @author Justin Deoliveira, Boundless
 */
public class XerialObjectDatabase extends SQLiteObjectDatabase<Connection> {

    static Logger LOG = LoggerFactory.getLogger(XerialObjectDatabase.class);

    static final String OBJECTS = "objects";

    final SQLiteDataSource dataSource;

    final int partitionSize = 10 * 1000; // TODO make configurable

    @Inject
    public XerialObjectDatabase(ConfigDatabase configdb, Platform platform) {
        this(configdb, platform, "objects");
    }

    public XerialObjectDatabase(ConfigDatabase configdb, Platform platform, String name) {
        super(configdb, platform);

        File db = new File(new File(platform.pwd(), ".geogit"), name + ".db");
        dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + db.getAbsolutePath());
    }

    @Override
    protected Connection connect() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("unable to open connection", e);
        }
    }

    @Override
    protected void close(Connection cx) {
        try {
            cx.close();
        } catch (SQLException e) {
            LOG.debug("error closing connection", e);
        }
    }

    @Override
    public void init(Connection cx) {
        new DbOp<Void>() {
            @Override
            protected Void doRun(Connection cx) throws SQLException {
                String sql = 
                    format("CREATE TABLE IF NOT EXISTS %s (id varchar, object blob)", OBJECTS);
                open(cx.createStatement()).execute(log(sql,LOG));

                sql = format("CREATE INDEX IF NOT EXISTS %s_id_index ON %s (id)", OBJECTS, OBJECTS);
                open(cx.createStatement()).execute(log(sql,LOG));

                return null;
            }
        }.run(cx);
    }

    @Override
    public boolean has(final String id, Connection cx) {
        return new DbOp<Boolean>() {
            @Override
            protected Boolean doRun(Connection cx) throws SQLException {
                String sql = format("SELECT count(*) FROM %s WHERE id = ?", OBJECTS);

                PreparedStatement ps = open(cx.prepareStatement(log(sql,LOG,id)));
                ps.setString(1, id);

                ResultSet rs = open(ps.executeQuery());
                rs.next();

                return rs.getInt(1) > 0;
            }
        }.run(cx);
    }

    @Override
    public Iterable<String> search(final String partialId, Connection cx) {
        final ResultSet rs = new DbOp<ResultSet>() {
            @Override
            protected ResultSet doRun(Connection cx) throws SQLException {
                String sql = 
                    format("SELECT id FROM %s WHERE id LIKE '%%%s%%'", OBJECTS, partialId);
                return cx.createStatement().executeQuery(log(sql,LOG));
            }
        }.run(cx);

        return new StringResultSetIterable(rs);
    }

    @Override
    public InputStream get(final String id, Connection cx) {
        return new DbOp<InputStream>() {
            @Override
            protected InputStream doRun(Connection cx) throws SQLException {
                String sql = format("SELECT object FROM %s WHERE id = ?", OBJECTS);

                PreparedStatement ps = open(cx.prepareStatement(log(sql,LOG,id)));
                ps.setString(1, id);
                
                ResultSet rs = open(ps.executeQuery());
                if (!rs.next()) {
                    return null;
                }

                byte[] bytes = rs.getBytes(1);
                return new ByteArrayInputStream(bytes);
            }
        }.run(cx);
    }

    @Override
    public void put(final String id, final InputStream obj, Connection cx) {
        new DbOp<Void>() {
            @Override
            protected Void doRun(Connection cx) throws SQLException, IOException {
                String sql = format("SELECT count(*) FROM %s WHERE id = ?", OBJECTS);

                PreparedStatement ps = open(cx.prepareStatement(log(sql,LOG,id)));
                ps.setString(1, id);

                ResultSet rs = open(ps.executeQuery());
                rs.next();
                if (rs.getInt(1) > 0) {
                    //update
                    sql = format("UPDATE %s SET object = ? WHERE id = ?", OBJECTS);
                }
                else {
                    //insert
                    sql = format("INSERT INTO %s (object,id) VALUES (?,?)", OBJECTS);
                }

                PreparedStatement upsert = open(cx.prepareStatement(log(sql,LOG,obj,id)));
                upsert.setBytes(1, ByteStreams.toByteArray(obj));
                upsert.setString(2, id);

                upsert.executeUpdate();
                return null;
            }
        }.run(cx);
    }

    @Override
    public boolean remove(final String id, Connection cx) {
        return new DbOp<Boolean>() {
            @Override
            protected Boolean doRun(Connection cx) throws SQLException {
                String sql = format("DELETE FROM %s WHERE id = ?", OBJECTS);

                PreparedStatement ps = open(cx.prepareStatement(log(sql,LOG,id)));
                ps.setString(1, id);

                return ps.executeUpdate() > 0;
            }
        }.run(cx);
    }

    /**
     * Override to optimize batch insert.
     */
    @Override
    public void putAll(final Iterator<? extends RevObject> objects, final BulkOpListener listener) {
        new DbOp<Void>() {
            @Override
            protected Void doRun(Connection cx) throws SQLException, IOException {
                cx.setAutoCommit(false);

                // use INSERT OR IGNORE to deal with duplicates cleanly
                String sql = format("INSERT OR IGNORE INTO %s (object,id) VALUES (?,?)", OBJECTS);
                PreparedStatement stmt = open(cx.prepareStatement(log(sql, LOG)));

                // partition the objects into chunks for batch processing 
                Iterator<List<? extends RevObject>> it =
                    (Iterator) Iterators.partition(objects, partitionSize);

                while (it.hasNext()) {
                    List<? extends RevObject> objs = it.next();
                    for (RevObject obj : objs) {
                        stmt.setBytes(1, ByteStreams.toByteArray(writeObject(obj)));
                        stmt.setString(2, obj.getId().toString());
                        stmt.addBatch();
                    }

                    notifyInserted(stmt.executeBatch(), objs, listener); 
                    stmt.clearParameters();
                }
                cx.commit();

                return null;
            }
        }.run(dataSource);
    }

    void notifyInserted(int[] inserted, List<? extends RevObject> objects, BulkOpListener listener) {
        for (int i = 0; i < inserted.length; i++) {
            if (inserted[i] > 0) {
                listener.inserted(objects.get(i).getId(), null);
            }
        }
    }

    /**
     * Override to optimize batch delete.
     */
    @Override
    public long deleteAll(final Iterator<ObjectId> ids, final BulkOpListener listener) {
        return new DbOp<Long>() {
            @Override
            protected Long doRun(Connection cx) throws SQLException, IOException {
                cx.setAutoCommit(false);

                String sql = format("DELETE FROM %s WHERE id = ?", OBJECTS);
                PreparedStatement stmt = open(cx.prepareStatement(log(sql, LOG)));

                long count = 0;

                // partition the objects into chunks for batch processing 
                Iterator<List<ObjectId>> it = Iterators.partition(ids, partitionSize);

                while (it.hasNext()) {
                    List<ObjectId> l = it.next();
                    for (ObjectId id : l) {
                        stmt.setString(1, id.toString());
                        stmt.addBatch();
                    }

                    count += notifyDeleted(stmt.executeBatch(), l, listener); 
                    stmt.clearParameters();
                }
                cx.commit();

                return count;
            }
        }.run(dataSource);
    }

    long notifyDeleted(int[] deleted, List<ObjectId> ids, BulkOpListener listener) {
        long count = 0; 
        for (int i = 0; i < deleted.length; i++) {
            if (deleted[i] > 0) {
                count++;
                listener.deleted(ids.get(i));
            }
        }
        return count;
    }
}

