/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.plumbing;

import org.geogit.api.RevFeatureType;
import org.geogit.test.integration.RepositoryTestCase;
import org.jeo.feature.Field;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

public class DescribeFeatureTypeTest extends RepositoryTestCase {

    RevFeatureType featureType;

    @Override
    protected void setUpInternal() throws Exception {
        featureType = RevFeatureType.build(pointsType);
    }

    @Test
    public void testDescribeNullFeatureType() throws Exception {
        DescribeFeatureType describe = new DescribeFeatureType();

        try {
            describe.call();
            fail("expected IllegalStateException on null feature type");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("FeatureType has not been set"));
        }
    }

    @Test
    public void testDescribeFeatureType() throws Exception {
        DescribeFeatureType describe = new DescribeFeatureType();

        ImmutableSet<Field> properties = describe.setFeatureType(featureType).call();

        for (Field prop : properties) {
            assertTrue(pointsType.getFields().contains(prop));
        }

    }
}
