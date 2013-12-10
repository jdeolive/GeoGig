/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.test.integration.sqlite;

import org.geogit.api.TestPlatform;
import org.geogit.test.integration.RevTreeBuilderTest;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import com.google.inject.Injector;

import static org.geogit.test.integration.sqlite.XerialTests.injector;

public class XerialRevTreeBuilderTest extends RevTreeBuilderTest {
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Override
    protected Injector createInjector() {
        return injector(new TestPlatform(temp.getRoot()));
    }
}
