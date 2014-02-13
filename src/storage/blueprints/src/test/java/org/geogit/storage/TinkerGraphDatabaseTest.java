/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage;

import org.geogit.di.GeogitModule;
import org.geogit.storage.blueprints.TinkerGraphDatabase;
import org.junit.Assert;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;


public class TinkerGraphDatabaseTest extends GraphDatabaseTest {

    @Override
    protected Injector createInjector() {
        Injector injector = Guice.createInjector(Modules.override(new GeogitModule()).with(
            new AbstractModule() {
                @Override
                protected void configure() {
                    bind(GraphDatabase.class).to(TinkerGraphDatabase.class);
                }
            }));
        
        Assert.assertTrue(injector.getInstance(GraphDatabase.class) instanceof TinkerGraphDatabase);
        return injector;
    }
}
