/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.sqlite;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.geogit.api.Platform;
import org.geogit.storage.ConfigDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteDataSource;

import com.google.inject.Inject;

import static org.geogit.storage.sqlite.SQLite.log;

/**
 * Graph database based on xerial SQLite jdbc driver.
 *
 * @author Justin Deoliveira, Boundless
 */
public class XerialGraphDatabase extends SQLiteGraphDatabase<Connection> {

    static Logger LOG = LoggerFactory.getLogger(XerialGraphDatabase.class);

    final SQLiteDataSource dataSource;

    @Inject
    public XerialGraphDatabase(ConfigDatabase configdb, Platform platform) {
        super(configdb, platform);

        //LOG.info(platform.pwd().getAbsolutePath());

        File db = new File(new File(platform.pwd(), ".geogit"), "graph.db");
        dataSource = Xerial.newDataSource(db);
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
            protected Void doRun(Connection cx) throws IOException, SQLException {
                Statement st = open(cx.createStatement());

                String sql = 
                    format("CREATE TABLE IF NOT EXISTS %s (id VARCHAR)", NODES);
                st.execute(log(sql,LOG));

                sql = format("CREATE INDEX IF NOT EXISTS %s_id_index ON %s(id)", NODES, NODES);
                st.execute(log(sql,LOG));

                sql = format("CREATE TABLE IF NOT EXISTS %s (src VARCHAR, dst VARCHAR)", EDGES);
                st.execute(log(sql,LOG));

                sql = format("CREATE INDEX IF NOT EXISTS %s_src_index ON %s(src)", EDGES, EDGES);
                st.execute(log(sql,LOG));

                sql = format("CREATE INDEX IF NOT EXISTS %s_dst_index ON %s(dst)", EDGES, EDGES);
                st.execute(log(sql,LOG));

                sql = format("CREATE TABLE IF NOT EXISTS %s (nid VARCHAR, key VARCHAR, val VARCHAR)", 
                    PROPS);
                st.execute(log(sql,LOG));

                sql = format("CREATE INDEX IF NOT EXISTS %s_nid_key_index ON %s(nid, key)", 
                    PROPS, PROPS);
                st.execute(log(sql,LOG));

                sql = format("CREATE TABLE IF NOT EXISTS %s (alias VARCHAR, nid VARCHAR)", MAPPINGS);
                st.execute(log(sql,LOG));

                sql = format("CREATE INDEX IF NOT EXISTS %s_alias_index ON %s(alias)", 
                    MAPPINGS, MAPPINGS);
                st.execute(log(sql,LOG));

                sql = format("CREATE INDEX IF NOT EXISTS %s_nid_index ON %s(nid)", 
                    MAPPINGS, MAPPINGS);
                st.execute(log(sql,LOG));

                return null;
            }
        }.run(cx);
        
    }

    @Override
    public boolean put(final String node, Connection cx) {
        if (has(node, cx)) {
            return false;
        }

        return new DbOp<Boolean>() {
            @Override
            protected Boolean doRun(Connection cx) throws IOException, SQLException {
                String sql = format("INSERT INTO %s (id) VALUES (?)", NODES);

                PreparedStatement ps = open(cx.prepareStatement(log(sql,LOG,node)));
                ps.setString(1, node);

                ps.executeUpdate();
                return true;
            }
        }.run(cx);
    }

    @Override
    public boolean has(final String node, Connection cx) {
        return new DbOp<Boolean>() {
            @Override
            protected Boolean doRun(Connection cx) throws IOException, SQLException {
                String sql = format("SELECT count(*) FROM %s WHERE id = ?", NODES);

                PreparedStatement ps = open(cx.prepareStatement(log(sql,LOG,node)));
                ps.setString(1, node);

                ResultSet rs = open(ps.executeQuery());
                rs.next();

                return rs.getInt(1) > 0;
            }
        }.run(cx);
    }

    @Override
    public void relate(final String src, final String dst, Connection cx) {
        new DbOp<Void>() {
            @Override
            protected Void doRun(Connection cx) throws IOException, SQLException {
                String sql = format("INSERT INTO %s (src, dst) VALUES (?, ?)", EDGES);

                PreparedStatement ps = open(cx.prepareStatement(log(sql,LOG,src,dst)));
                ps.setString(1, src);
                ps.setString(2, dst);

                ps.executeUpdate();
                return null;
            }
        }.run(cx);
    }

    @Override
    public void map(final String from, final String to, Connection cx) {
        new DbOp<Void>() {
            @Override
            protected Void doRun(Connection cx) throws IOException, SQLException {
                String sql = format("SELECT count(*) FROM %s WHERE alias = ?", MAPPINGS);

                PreparedStatement ps = open(cx.prepareStatement(log(sql,LOG,from)));
                ps.setString(1, from);

                ResultSet rs = open(ps.executeQuery());
                rs.next();
                if (rs.getInt(1) > 0) {
                    // update
                    sql = format("UPDATE %s SET nid = ? WHERE alias = ?", MAPPINGS);
                }
                else {
                    // insert
                    sql = format("INSERT INTO %s (nid, alias) VALUES (?, ?)", MAPPINGS);
                }

                ps = open(cx.prepareStatement(log(sql,LOG,to,from)));
                ps.setString(1, to);
                ps.setString(2, from);

                ps.executeUpdate();
                return null;
            }
        }.run(cx);
    }

    @Override
    public String mapping(final String node, Connection cx) {
        return new DbOp<String>() {
            @Override
            protected String doRun(Connection cx) throws IOException, SQLException {
                String sql = format("SELECT nid FROM %s WHERE alias = ?", MAPPINGS);

                PreparedStatement ps = open(cx.prepareStatement(log(sql,LOG,node)));
                ps.setString(1, node);

                ResultSet rs = open(ps.executeQuery());
                return rs.next() ? rs.getString(1) : null;
            }
        }.run(cx);
    }

    @Override
    public void property(final String node, final String key, final String val, Connection cx) {
        new DbOp<Void>() {
            @Override
            protected Void doRun(Connection cx) throws IOException, SQLException {
                String sql = format("SELECT count(*) FROM %s WHERE nid = ? AND key = ?", PROPS);

                PreparedStatement ps = open(cx.prepareStatement(log(sql,LOG,node,key)));
                ps.setString(1, node);
                ps.setString(2, key);

                ResultSet rs = open(ps.executeQuery());
                if (rs.getInt(1) == 0) {
                    // insert
                    sql = format("INSERT INTO %s (val, nid, key) VALUES (?,?,?)", PROPS);
                }
                else {
                    // update
                    sql = format("UPDATE %s SET val = ? WHERE nid = ? AND key = ?", PROPS);
                }

                ps = open(cx.prepareStatement(log(sql,LOG,node,key,val)));
                ps.setString(1, val);
                ps.setString(2, node);
                ps.setString(3, key);

                ps.executeUpdate();
                return null;
            }
        }.run(cx);
    }

    @Override
    public String property(final String node, final String key, Connection cx) {
        return new DbOp<String>() {
            @Override
            protected String doRun(Connection cx) throws IOException, SQLException {
                String sql = format("SELECT val FROM %s WHERE nid = ? AND key = ?", PROPS);

                PreparedStatement ps = open(cx.prepareStatement(log(sql,LOG,node,key)));
                ps.setString(1, node);
                ps.setString(2, key);

                ResultSet rs = open(ps.executeQuery());
                if (rs.next()) {
                    return rs.getString(1);
                }
                else {
                    return null;
                }
            }
        }.run(cx);
    }

    @Override
    public Iterable<String> outgoing(final String node, Connection cx) {
        ResultSet rs = new DbOp<ResultSet>() {
            @Override
            protected ResultSet doRun(Connection cx) throws IOException, SQLException {
                String sql = format("SELECT dst FROM %s WHERE src = ?", EDGES);

                PreparedStatement ps = cx.prepareStatement(log(sql,LOG,node));
                ps.setString(1, node);
                return ps.executeQuery();
            }
        }.run(cx);

        return new StringResultSetIterable(rs);

    }

    @Override
    public Iterable<String> incoming(final String node, Connection cx) {
        ResultSet rs = new DbOp<ResultSet>() {
            @Override
            protected ResultSet doRun(Connection cx) throws IOException, SQLException {
                String sql = format("SELECT src FROM %s WHERE dst = ?", EDGES);

                PreparedStatement ps = cx.prepareStatement(log(sql,LOG,node));
                ps.setString(1, node);
                return ps.executeQuery();
            }
        }.run(cx);

        return new StringResultSetIterable(rs);
    }

    @Override
    public void clear(Connection cx) {
        new DbOp<Void>() {
            @Override
            protected Void doRun(Connection cx) throws IOException, SQLException {
                Statement st = open(cx.createStatement());

                String sql = format("DELETE FROM %s", PROPS);
                st.execute(log(sql,LOG));

                sql = format("DELETE FROM %s", EDGES);
                st.execute(log(sql,LOG));

                sql = format("DELETE FROM %s", NODES);
                st.execute(log(sql,LOG));

                return null;
            }
        }.run(cx);
    }

}
