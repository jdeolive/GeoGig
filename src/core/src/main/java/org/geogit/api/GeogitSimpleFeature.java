/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jeo.feature.Feature;
import org.jeo.feature.Field;
import org.jeo.feature.Schema;
import org.jeo.util.Convert;
import org.osgeo.proj4j.CoordinateReferenceSystem;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Geometry;

/**
 */
public class GeogitSimpleFeature implements Feature {

    private final String id;
    private final String version;

    private Schema featureType;

    /**
     * The actual values held by this feature
     */
    private List<Optional<Object>> revFeatureValues;

    /**
     * The attribute name -> position index
     */
    private Map<String, Integer> nameToRevTypeIndex;

    private BiMap<Integer, Integer> typeToRevTypeIndex;

    /**
     * The set of user data attached to the feature (lazily created)
     */
    private Map<Object, Object> userData;

    /**
     * The set of user data attached to each attribute (lazily created)
     */
    private Map<Object, Object>[] attributeUserData;

    /**
     * Fast construction of a new feature.
     * <p>
     * The object takes ownership of the provided value array, do not modify after calling the
     * constructor
     * </p>
     * 
     * @param values
     * @param featureType
     * @param id
     * @param validating
     * @param nameToRevTypeInded - attribute name to value index mapping
     */
    public GeogitSimpleFeature(ImmutableList<Optional<Object>> values,
            Schema featureType, String id, String version, Map<String, Integer> nameToRevTypeInded,
            BiMap<Integer, Integer> typeToRevTypeIndex) {
        this.id = id;
        this.version = version;
        this.featureType = featureType;
        this.revFeatureValues = values;
        this.nameToRevTypeIndex = nameToRevTypeInded;
        this.typeToRevTypeIndex = typeToRevTypeIndex;
    }

    private List<Optional<Object>> mutableValues() {
        if (revFeatureValues instanceof ImmutableList) {
            revFeatureValues = Lists.newArrayList(revFeatureValues);
        }
        return revFeatureValues;
    }

    @Override
    public String getId() {
        return id;
    }

    public int getNumberOfAttributes() {
        return revFeatureValues.size();
    }

    @Override
    public Object get(String name) {
        Integer revTypeIndex = nameToRevTypeIndex.get(name);
        if (revTypeIndex != null)
            return revFeatureValues.get(revTypeIndex).orNull();
        else
            return null;
    }

    @Override
    public Object get(int index) {
        int revTypeIndex = typeToRevTypeIndex(index);
        return revFeatureValues.get(revTypeIndex).orNull();
    }

    private int typeToRevTypeIndex(int index) {
        int revTypeIndex = typeToRevTypeIndex.get(Integer.valueOf(index)).intValue();
        return revTypeIndex;
    }

    public int getAttributeCount() {
        return revFeatureValues.size();
    }

    @Override
    public List<Object> list() {
        final int attributeCount = getAttributeCount();
        List<Object> atts = new ArrayList<Object>(attributeCount);
        for (int i = 0; i < attributeCount; i++) {
            atts.add(get(i));
        }
        return atts;
    }

    @Override
    public Map<String, Object> map() {
        Map<String,Object> map = Maps.newLinkedHashMap();
        for (Field f : featureType) {
            map.put(f.getName(), get(f.getName()));
        }
        return map;
    }

    @Override
    public Geometry geometry() {
        // should be specified in the index as the default key (null)
        Integer idx = nameToRevTypeIndex.get(null);
        Object defaultGeometry = idx != null ? revFeatureValues.get(idx).orNull() : null;

        // not found? do we have a default geometry at all?
        if (defaultGeometry == null) {
            Field geometryDescriptor = featureType.geometry();
            if (geometryDescriptor != null) {
                Integer defaultGeomIndex = nameToRevTypeIndex.get(geometryDescriptor.getName());
                defaultGeometry = revFeatureValues.get(defaultGeomIndex.intValue()).get();
            }
        }

        //TODO: can geogit really handle non jts geometries?
        return (Geometry) defaultGeometry;
    }

    @Override
    public Schema schema() {
        return featureType;
    }

    
    @Override
    public void set(int index, Object value) {
        // first do conversion
        @SuppressWarnings("rawtypes") Class binding = schema().getFields().get(index).getType();
        @SuppressWarnings("unchecked") Object converted = 
            Convert.to(value, binding).or(binding.cast(value));

        // finally set the value into the feature
        Integer revFeatureIndex = typeToRevTypeIndex.get(index);
        mutableValues().set(revFeatureIndex.intValue(), Optional.fromNullable(converted));
    }

    @Override
    public void put(String name, Object value) {
        final Integer revTypeIndex = nameToRevTypeIndex.get(name);
        if (revTypeIndex == null) {
            throw new IllegalArgumentException("Unknown attribute " + name);
        }
        Integer schemaIndex = typeToRevTypeIndex.inverse().get(revTypeIndex);
        set(schemaIndex, value);
    }

    @Override
    public void put(Geometry geometry) {
        Integer geometryIndex = nameToRevTypeIndex.get(null);
        if (geometryIndex != null) {
            mutableValues().set(geometryIndex.intValue(), Optional.fromNullable((Object)geometry));
        }
    }

    @Override
    public CoordinateReferenceSystem getCRS() {
        return crs();
    }

    @Override
    public void setCRS(CoordinateReferenceSystem crs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CoordinateReferenceSystem crs() {
        return featureType.crs();
    }

    /**
     * returns a unique code for this feature
     * 
     * @return A unique int
     */
    public int hashCode() {
        return id.hashCode() * featureType.hashCode();
    }

    /**
     * override of equals. Returns if the passed in object is equal to this.
     * 
     * @param obj the Object to test for equality.
     * 
     * @return <code>true</code> if the object is equal, <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (!(obj instanceof GeogitSimpleFeature)) {
            return false;
        }

        GeogitSimpleFeature feat = (GeogitSimpleFeature) obj;

        if (!id.equals(feat.getId())) {
            return false;
        }

        if (!feat.schema().equals(featureType)) {
            return false;
        }

        for (int i = 0, ii = revFeatureValues.size(); i < ii; i++) {
            Object otherAtt = feat.get(i);

            if (!Objects.equal(otherAtt, get(i))) {
                return false;
            }
        }

        return true;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append(schema().getName());
        sb.append('=');
        sb.append(list());
        return sb.toString();
    }

    public static Map<String, Integer> buildAttNameToRevTypeIndex(RevFeatureType revType) {

        List<Field> sortedDescriptors = revType.sortedDescriptors();

        Map<String, Integer> typeAttNameToRevTypeIndex = Maps.newHashMap();

        final Field defaultGeometry = revType.type().geometry();
        for (int revFeatureIndex = 0; revFeatureIndex < sortedDescriptors.size(); revFeatureIndex++) {
            Field prop = sortedDescriptors.get(revFeatureIndex);
            typeAttNameToRevTypeIndex.put(prop.getName(), Integer.valueOf(revFeatureIndex));

            if (prop.equals(defaultGeometry)) {
                typeAttNameToRevTypeIndex.put(null, Integer.valueOf(revFeatureIndex));
            }

        }

        return typeAttNameToRevTypeIndex;
    }

    public static BiMap<Integer, Integer> buildTypeToRevTypeIndex(RevFeatureType revType) {

        List<Field> sortedDescriptors = revType.sortedDescriptors();
        List<Field> unsortedDescriptors = ImmutableList.copyOf(revType.type().getFields());

        Map<Integer, Integer> typeToRevTypeIndex = Maps.newHashMap();
        for (int revFeatureIndex = 0; revFeatureIndex < sortedDescriptors.size(); revFeatureIndex++) {
            Field prop = sortedDescriptors.get(revFeatureIndex);
            int typeIndex = unsortedDescriptors.indexOf(prop);
            typeToRevTypeIndex.put(Integer.valueOf(typeIndex), Integer.valueOf(revFeatureIndex));
        }
        return ImmutableBiMap.copyOf(typeToRevTypeIndex);
    }

}
