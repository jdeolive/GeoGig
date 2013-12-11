/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.sqlite;

import static java.lang.String.format;
import static org.geogit.storage.sqlite.SQLite.log;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.geogit.api.Platform;
import org.geogit.storage.ConfigDatabase;
import org.geogit.storage.ObjectDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * Staging database based on Xerial SQLite jdbc driver.
 *
 * @author Justin Deoliveira, Boundless
 */
public class XerialStagingDatabase extends SQLiteStagingDatabase<Connection> {

    final static Logger LOG = LoggerFactory.getLogger(XerialStagingDatabase.class);

    final static String CONFLICTS = "conflicts";

    @Inject
    public XerialStagingDatabase(ObjectDatabase repoDb, ConfigDatabase configdb, Platform platform) {
        super(repoDb, new XerialObjectDatabase(configdb, platform, "stage"), configdb, platform);
    }

    @Override
    protected void init(Connection cx) {
        new DbOp<Void>() {
            @Override
            protected Void doRun(Connection cx) throws SQLException {
                String sql = format("CREATE TABLE IF NOT EXISTS %s (namespace VARCHAR, "
                    + "path VARCHAR, conflict VARCHAR)", CONFLICTS);
                
                LOG.debug(sql);
                open(cx.createStatement()).execute(sql);

                return null;
            }
        }.run(cx);
    }

    @Override
    protected Iterable<String> get(final String namespace, final String pathFilter, Connection cx) {
        ResultSet rs = new DbOp<ResultSet>() {
            @Override
            protected ResultSet doRun(Connection cx) throws IOException, SQLException {
                String sql = format(
                    "SELECT conflict FROM %s WHERE namespace = ? AND path LIKE '%%%s%%'", 
                    CONFLICTS, pathFilter);

                PreparedStatement ps = open(cx.prepareStatement(log(sql,LOG,namespace)));
                ps.setString(1, namespace);

                return ps.executeQuery();
            }
        }.run(cx);

        return new StringResultSetIterable(rs);
    }

    @Override
    protected void put(final String namespace, final String path, final String conflict, 
        Connection cx) {
        new DbOp<Void>() {
            @Override
            protected Void doRun(Connection cx) throws IOException, SQLException {
                String sql = format("INSERT INTO %s VALUES (?,?,?)", CONFLICTS);

                log(sql, LOG, namespace, path, conflict);

                PreparedStatement ps = open(cx.prepareStatement(sql));
                ps.setString(1, namespace);
                ps.setString(2, path);
                ps.setString(3, conflict);

                ps.executeUpdate();
                return null;
            }
        }.run(cx);
    }

    @Override
    protected void remove(final String namespace, final String path, Connection cx) {
        new DbOp<Void>() {
            @Override
            protected Void doRun(Connection cx) throws IOException, SQLException {
                String sql = format("DELETE FROM %s WHERE namespace = ? AND path = ?", CONFLICTS);

                log(sql, LOG, namespace, path);

                PreparedStatement ps = open(cx.prepareStatement(sql));
                ps.setString(1, namespace);
                ps.setString(2, path);

                ps.executeUpdate();
                return null;
            }
        }.run(cx);
    }

}
