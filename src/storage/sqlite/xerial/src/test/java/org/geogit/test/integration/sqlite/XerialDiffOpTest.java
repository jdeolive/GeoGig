/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.test.integration.sqlite;

import static org.geogit.storage.sqlite.Xerial.injector;

import org.geogit.test.integration.DiffOpTest;

import com.google.inject.Injector;

public class XerialDiffOpTest extends DiffOpTest {
    @Override
    protected Injector createInjector() {
        return injector();
    }
}
