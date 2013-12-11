/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.sqlite;

import org.geogit.di.GeogitModule;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

/**
 * Utility class.
 * 
 * @author Justin Deoliveira, Boundless
 *
 */
public class Xerial {

    /**
     * Creates the injector to enable xerial sqlite storage.
     */
    public static Injector injector() {
        return Guice.createInjector(
            Modules.override(new GeogitModule()).with(new XerialSQLiteModule()));
    }
}
