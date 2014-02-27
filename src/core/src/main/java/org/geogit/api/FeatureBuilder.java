/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.api;

import java.util.Map;

import org.jeo.feature.Feature;
import org.jeo.feature.Schema;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableList;

/**
 * Provides a method of building features from {@link RevFeature} objects that have the type
 * specified by the given {@link RevFeatureType}.
 * 
 * @see RevFeatureType
 * @see RevFeature
 * @see Feature
 */
public class FeatureBuilder {

    private Schema featureType;

    private Map<String, Integer> attNameToRevTypeIndex;

    private BiMap<Integer, Integer> typeToRevTypeIndex;

    private RevFeatureType type;

    /**
     * Constructs a new {@code FeatureBuilder} with the given {@link RevFeatureType feature type}.
     * 
     * @param type the feature type of the features that will be built
     */
    public FeatureBuilder(RevFeatureType type) {
        this.type = type;
        this.featureType = type.type();
        this.attNameToRevTypeIndex = GeogitSimpleFeature.buildAttNameToRevTypeIndex(type);
        this.typeToRevTypeIndex = GeogitSimpleFeature.buildTypeToRevTypeIndex(type);
    }

    public RevFeatureType getType() {
        return type;
    }

    /**
     * Constructs a new {@code FeatureBuilder} with the given {@link SimpleFeatureType feature type}
     * .
     * 
     * @param type the feature type of the features that will be built
     */
    public FeatureBuilder(Schema type) {
        this(RevFeatureType.build(type));
    }

    /**
     * Builds a {@link Feature} from the provided {@link RevFeature}.
     * 
     * @param id the id of the new feature
     * @param revFeature the {@code RevFeature} with the property values for the feature
     * @return the constructed {@code Feature}
     */
    public Feature build(final String id, final RevFeature revFeature) {
        Preconditions.checkNotNull(id);
        Preconditions.checkNotNull(revFeature);

        final String version = revFeature.getId().toString();
        
        ImmutableList<Optional<Object>> values = revFeature.getValues();
        GeogitSimpleFeature feature = new GeogitSimpleFeature(values,
            featureType, id, version, attNameToRevTypeIndex, typeToRevTypeIndex);
        return feature;
    }

}
