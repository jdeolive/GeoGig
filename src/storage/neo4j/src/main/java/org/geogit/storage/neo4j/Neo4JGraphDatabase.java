/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.neo4j;

import java.util.Map;

import org.geogit.api.Platform;
import org.geogit.repository.RepositoryConnectionException;
import org.geogit.storage.ConfigDatabase;
import org.geogit.storage.blueprints.TransactionalBlueprintsGraphDatabase;

import com.google.inject.Inject;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;

/**
 * Provides an implementation of a GeoGit Graph Database using Neo4J.
 */
public class Neo4JGraphDatabase extends TransactionalBlueprintsGraphDatabase<Neo4jGraph> {
    private final ConfigDatabase configDB;

    /**
     * Constructs a new {@code Neo4JGraphDatabase} using the given platform.
     * 
     * @param platform the platform to use.
     */
    @Inject
    public Neo4JGraphDatabase(final Platform platform, ConfigDatabase configDB) {
        super(platform);
        this.configDB = configDB;
    }

    @Override
    protected Neo4jGraph getGraphDatabase() {
        Map<String, String> settings = new java.util.HashMap<String, String>();
        // GR: please add a note on why this setting is needed
        settings.put("online_backup_enabled", "false");
        return new Neo4jGraph(dbPath, settings);
    }

    @Override
    public void configure() throws RepositoryConnectionException {
        RepositoryConnectionException.StorageType.GRAPH.configure(configDB, "neo4j", "0.1");
    }

    @Override
    public void checkConfig() throws RepositoryConnectionException {
        RepositoryConnectionException.StorageType.GRAPH.verify(configDB, "neo4j", "0.1");
    }
}
