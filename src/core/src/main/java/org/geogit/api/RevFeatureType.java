/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api;

import java.util.ArrayList;

import org.geogit.api.plumbing.HashObject;
import org.opengis.feature.type.Name;
import org.jeo.feature.Field;
import org.jeo.feature.Schema;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * A binary representation of the state of a Feature Type.
 */
public class RevFeatureType extends AbstractRevObject {

    private final Schema featureType;

    private ImmutableList<Field> sortedDescriptors;

    public static RevFeatureType build(Schema type) {
        RevFeatureType unnamed = new RevFeatureType(type);
        ObjectId id = new HashObject().setObject(unnamed).call();
        return new RevFeatureType(id, type);
    }

    /**
     * Constructs a new {@code RevFeatureType} from the given {@link FeatureType}.
     * 
     * @param featureType the feature type to use
     */
    private RevFeatureType(Schema featureType) {
        this(ObjectId.NULL, featureType);
    }

    /**
     * Constructs a new {@code RevFeatureType} from the given {@link ObjectId} and
     * {@link FeatureType}.
     * 
     * @param id the object id to use for this feature type
     * @param featureType the feature type to use
     */
    public RevFeatureType(ObjectId id, Schema featureType) {
        super(id);
        this.featureType = featureType;
        ArrayList<Field> descriptors = Lists.newArrayList(this.featureType.getFields());
        sortedDescriptors = ImmutableList.copyOf(descriptors);

    }

    @Override
    public TYPE getType() {
        return TYPE.FEATURETYPE;
    }

    public Schema type() {
        return featureType;
    }

    /**
     * @return the sorted {@link PropertyDescriptor}s of the feature type
     */
    public ImmutableList<Field> sortedDescriptors() {
        return sortedDescriptors;
    }

    /**
     * @return the name of the feature type
     */
    public Name getName() {
        Schema type = type();
        return new Name(type.getURI(), type.getName());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FeatureType[");
        builder.append(getId().toString());
        builder.append("; ");
        boolean first = true;
        for (Field desc : sortedDescriptors()) {
            if (first) {
                first = false;
            } else {
                builder.append(", ");
            }
            builder.append(desc.getName());
            builder.append(": ");
            builder.append(desc.getType().getSimpleName());
        }
        builder.append(']');
        return builder.toString();
    }
}
