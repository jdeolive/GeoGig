/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.plumbing.diff;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Map;

import org.geogit.api.NodeRef;
import org.geogit.api.RevFeatureType;
import org.geogit.test.integration.RepositoryTestCase;
import org.jeo.feature.Field;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;

public class PatchSerializationTest extends RepositoryTestCase {

    @Override
    protected void setUpInternal() throws Exception {
    }

    @Test
    public void testRemoveFeatureAttributePatch() throws Exception {
        Patch patch = new Patch();
        String path = NodeRef.appendChild(pointsName, points1.getId());
        Map<Field, AttributeDiff> map = Maps.newHashMap();
        Optional<?> oldValue = Optional.fromNullable(points1B.get("extra"));
        GenericAttributeDiffImpl diff = new GenericAttributeDiffImpl(oldValue, null);
        map.put(modifiedPointsType.field("extra"), diff);
        FeatureDiff featureDiff = new FeatureDiff(path, map,
                RevFeatureType.build(modifiedPointsType), RevFeatureType.build(pointsType));
        patch.addModifiedFeature(featureDiff);
        patch.addFeatureType(RevFeatureType.build(pointsType));
        testPatch(patch);
    }

    @Test
    public void testAddFeatureAttributePatch() throws Exception {
        Patch patch = new Patch();
        String path = NodeRef.appendChild(pointsName, points1.getId());
        Map<Field, AttributeDiff> map = Maps.newHashMap();
        Optional<?> newValue = Optional.fromNullable(points1B.get("extra"));
        GenericAttributeDiffImpl diff = new GenericAttributeDiffImpl(null, newValue);
        map.put(modifiedPointsType.field("extra"), diff);
        FeatureDiff featureDiff = new FeatureDiff(path, map, RevFeatureType.build(pointsType),
                RevFeatureType.build(modifiedPointsType));
        patch.addModifiedFeature(featureDiff);
        patch.addFeatureType(RevFeatureType.build(modifiedPointsType));
        testPatch(patch);
    }

    @Test
    public void testModifyFeatureAttributePatch() throws Exception {
        Patch patch = new Patch();
        String path = NodeRef.appendChild(pointsName, points1.getId());
        Map<Field, AttributeDiff> map = Maps.newHashMap();
        Optional<?> oldValue = Optional.fromNullable(points1.get("sp"));
        GenericAttributeDiffImpl diff = new GenericAttributeDiffImpl(oldValue, Optional.of("new"));
        Optional<Geometry> oldGeometry = Optional.fromNullable((Geometry) points1.get("pp"));
        Optional<Geometry> newGeometry = Optional.of(new WKTReader().read("POINT (2 2)"));
        GeometryAttributeDiff geomDiff = new GeometryAttributeDiff(oldGeometry, newGeometry);
        map.put(pointsType.field("sp"), diff);
        map.put(pointsType.field("pp"), geomDiff);
        FeatureDiff feaureDiff = new FeatureDiff(path, map, RevFeatureType.build(pointsType),
                RevFeatureType.build(pointsType));
        patch.addModifiedFeature(feaureDiff);
        testPatch(patch);
    }

    @Test
    public void testAddFeaturePatch() throws Exception {
        Patch patch = new Patch();
        String path = NodeRef.appendChild(pointsName, points1.getId());
        patch.addAddedFeature(path, points1, RevFeatureType.build(pointsType));
        testPatch(patch);
    }

    @Test
    public void testRemoveFeaturePatch() throws Exception {
        Patch patch = new Patch();
        String path = NodeRef.appendChild(pointsName, points1.getId());
        patch.addRemovedFeature(path, points1, RevFeatureType.build(pointsType));
        testPatch(patch);
    }

    @Test
    public void testModifiedEmptyFeatureTypePatch() throws Exception {
        Patch patch = new Patch();
        RevFeatureType featureType = RevFeatureType.build(pointsType);
        RevFeatureType modifiedFeatureType = RevFeatureType.build(modifiedPointsType);
        patch.addFeatureType(featureType);
        patch.addFeatureType(modifiedFeatureType);
        patch.addAlteredTree(new FeatureTypeDiff(pointsName, featureType.getId(),
                modifiedFeatureType.getId()));
        testPatch(patch);
    }

    @Test
    public void testAddEmptyFeatureTypePatch() throws Exception {
        Patch patch = new Patch();
        RevFeatureType featureType = RevFeatureType.build(pointsType);
        patch.addFeatureType(featureType);
        patch.addAlteredTree(new FeatureTypeDiff(pointsName, null, featureType.getId()));
        testPatch(patch);
    }

    @Test
    public void testRemoveEmptyFeatureTypePatch() throws Exception {
        Patch patch = new Patch();
        RevFeatureType featureType = RevFeatureType.build(pointsType);
        patch.addFeatureType(featureType);
        patch.addAlteredTree(new FeatureTypeDiff(pointsName, featureType.getId(), null));
        testPatch(patch);
    }

    private void testPatch(Patch patch) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(out, Charsets.UTF_8);
        PatchSerializer.write(writer, patch);
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        Patch serializedPatch = PatchSerializer.read(reader);
        assertEquals(patch, serializedPatch);
    }

}
