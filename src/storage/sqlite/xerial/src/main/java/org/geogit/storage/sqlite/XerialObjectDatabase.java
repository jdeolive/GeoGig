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

import org.geogit.api.Platform;
import org.geogit.storage.ConfigDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteDataSource;

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
}
