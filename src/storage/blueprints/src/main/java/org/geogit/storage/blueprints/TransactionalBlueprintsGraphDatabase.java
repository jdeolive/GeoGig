/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.blueprints;

import org.geogit.api.Platform;

import com.tinkerpop.blueprints.KeyIndexableGraph;
import com.tinkerpop.blueprints.TransactionalGraph;

/**
 * An abstract {@link BlueprintsGraphDatabase} for implementations capable of performing graph
 * transactions, with overrides for the transaction handling methods.
 * 
 * @param <DB>
 */
public abstract class TransactionalBlueprintsGraphDatabase<DB extends KeyIndexableGraph & TransactionalGraph>
        extends BlueprintsGraphDatabase<DB> {
    public TransactionalBlueprintsGraphDatabase(Platform platform) {
        super(platform);
    }

    @Override
    protected void commit() {
        graphDB.commit();
    }

    @Override
    protected void rollback() {
        graphDB.rollback();
    }
}
