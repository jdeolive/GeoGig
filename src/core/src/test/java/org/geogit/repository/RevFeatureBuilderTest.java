/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.repository;

import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureBuilder;
import org.geogit.test.integration.RepositoryTestCase;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class RevFeatureBuilderTest extends RepositoryTestCase {

    @Override
    protected void setUpInternal() throws Exception {

    }

    @Test
    public void testBuildEmpty() throws Exception {
        RevFeatureBuilder b = new RevFeatureBuilder();

        try {
            b.build(null);
            fail("expected IllegalStateException on null feature");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("No feature set"));
        }
    }

    @Test
    public void testBuildFull() throws Exception {
        RevFeatureBuilder b = new RevFeatureBuilder();

        RevFeature feature = b.build(points1);

        ImmutableList<Optional<Object>> values = feature.getValues();

        assertEquals(values.size(), points1.list().size());

        for (Object prop : points1.list()) {
            assertTrue(values.contains(Optional.fromNullable(prop)));
        }

        RevFeature feature2 = b.build(lines1);

        values = feature2.getValues();

        assertEquals(values.size(), lines1.list().size());

        for (Object prop : lines1.list()) {
            assertTrue(values.contains(Optional.fromNullable(prop)));
        }

    }
}
