/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.storage.blueprints;

import org.geogit.storage.GraphDatabase;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

/**
 *
 */
public class BlueprintsGraphModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(GraphDatabase.class).to(TinkerGraphDatabase.class).in(Scopes.SINGLETON);
    }

}
