/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.RevFeatureType;
import org.jeo.feature.Field;
import org.jeo.feature.Schema;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

/**
 * Retrieves the set of property descriptors for the given feature type.
 */
public class DescribeFeatureType extends AbstractGeoGitOp<ImmutableSet<Field>> {

    private RevFeatureType featureType;

    /**
     * @param featureType the {@link RevFeatureType} to describe
     */
    public DescribeFeatureType setFeatureType(RevFeatureType featureType) {
        this.featureType = featureType;
        return this;
    }

    /**
     * Retrieves the set of property descriptors for the given feature type.
     * 
     * @return a sorted set of all the property descriptors of the feature type.
     */
    @Override
    public ImmutableSet<Field> call() {
        Preconditions.checkState(featureType != null, "FeatureType has not been set.");

        Schema type = featureType.type();

        ImmutableSet.Builder<Field> propertySetBuilder = new ImmutableSet.Builder<Field>();

        propertySetBuilder.addAll(type.getFields());

        return propertySetBuilder.build();
    }
}
