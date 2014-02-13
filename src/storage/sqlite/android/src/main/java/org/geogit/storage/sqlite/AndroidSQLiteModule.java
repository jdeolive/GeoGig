/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.sqlite;

import org.geogit.storage.ConfigDatabase;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.StagingDatabase;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class AndroidSQLiteModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ConfigDatabase.class).to(AndroidConfigDatabase.class).in(Scopes.SINGLETON);
        bind(ObjectDatabase.class).to(AndroidObjectDatabase.class).in(Scopes.SINGLETON);
        bind(StagingDatabase.class).to(AndroidStagingDatabase.class).in(Scopes.SINGLETON);
        //bind(GraphDatabase.class).to(AndroidGraphDatabase.class).in(Scopes.SINGLETON);
    }

}
