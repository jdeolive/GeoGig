/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.cli;

import org.geogit.api.InjectorBuilder;
import org.geogit.di.GeogitModule;
import org.geogit.di.caching.CachingModule;
import org.geogit.metrics.MetricsModule;
import org.geogit.storage.bdbje.JEStorageModule;
import org.geogit.storage.blueprints.BlueprintsGraphModule;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

public class CLIInjectorBuilder extends InjectorBuilder {

    @Override
    public Injector build() {
        return Guice.createInjector(Modules.override(new GeogitModule(), new CachingModule()).with(
                new JEStorageModule(), new MetricsModule(), new BlueprintsGraphModule()));
    }

}
