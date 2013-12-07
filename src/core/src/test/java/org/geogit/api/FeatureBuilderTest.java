/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api;

import org.geogit.test.integration.RepositoryTestCase;
import org.jeo.feature.Feature;
import org.junit.Test;

public class FeatureBuilderTest extends RepositoryTestCase {

    @Override
    protected void setUpInternal() throws Exception {
        repo.getConfigDatabase().put("user.name", "groldan");
        repo.getConfigDatabase().put("user.email", "groldan@opengeo.org");
    }

    @Test
    public void testFeatureBuilder() {
        FeatureBuilder builder = new FeatureBuilder(pointsType);
        RevFeatureBuilder revBuilder = new RevFeatureBuilder();
        RevFeature point1 = revBuilder.build(points1);

        Feature test = builder.build(idP1, point1);

        // assertEquals(points1.getValue(), test.getValue());
        assertEquals(points1.getId(), test.getId());
        assertEquals(points1.schema(), test.schema());
        //assertEquals(points1.getUserData(), test.getUserData());

        RevFeature feature = revBuilder.build(test);
        Feature test2 = builder.build(idP1, feature);

        assertEquals(test.list(), test2.list());
    }
}
