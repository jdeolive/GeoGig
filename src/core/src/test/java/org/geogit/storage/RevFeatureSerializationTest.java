/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureBuilder;
import org.geogit.api.RevObject.TYPE;
import org.jeo.feature.BasicFeature;
import org.jeo.feature.Feature;
import org.jeo.feature.Schema;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

public abstract class RevFeatureSerializationTest extends Assert {
    private String namespace1 = "http://geoserver.org/test";

    private String typeName1 = "TestType";

    private String typeSpec1 = "str:String," + //
            "bool:Boolean," + //
            "byte:java.lang.Byte," + //
            "doub:Double," + //
            "bdec:java.math.BigDecimal," + //
            "flt:Float," + //
            "int:Integer," + //
            "bint:java.math.BigInteger," + //
            "pp:Point:srid=4326," + //
            "lng:java.lang.Long," + //
            "datetime:java.util.Date," + //
            "date:java.sql.Date," + //
            "time:java.sql.Time," + //
            "timestamp:java.sql.Timestamp," + //
            "uuid:java.util.UUID";

    protected Schema featureType1;

    private Feature feature1_1;

    protected ObjectSerializingFactory factory = getObjectSerializingFactory();

    protected abstract ObjectSerializingFactory getObjectSerializingFactory();

    @Before
    public void initializeFeatureAndFeatureType() throws Exception {
        /* now we will setup our feature types and test features. */
        featureType1 = Schema.build(typeName1).uri(namespace1).fields(typeSpec1).schema();
        // have to store timestamp in a variable since the nanos field is only accessible via setter
        // and getter
        java.sql.Timestamp timestamp = new java.sql.Timestamp(1264396155228L);
        timestamp.setNanos(23456);
        feature1_1 = feature(featureType1, //
                "TestType.feature.1", //
                "StringProp1_1", //
                Boolean.TRUE, //
                Byte.valueOf("18"), //
                new Double(100.01), //
                new BigDecimal("1.89e1021"), //
                new Float(12.5), //
                new Integer(1000), //
                new BigInteger("90000000"), //
                "POINT(1 1)", //
                new Long(800000), //
                new java.util.Date(1264396155228L), //
                new java.sql.Date(1364356800000L), //
                new java.sql.Time(57355228L), //
                timestamp, //
                UUID.fromString("bd882d24-0fe9-11e1-a736-03b3c0d0d06d"));
    }

    @Test
    public void testSerialize() throws Exception {
        testFeatureReadWrite(feature1_1);
    }

    protected void testFeatureReadWrite(Feature feature) throws Exception {

        RevFeatureBuilder builder = new RevFeatureBuilder();
        RevFeature newFeature = builder.build(feature);
        ObjectWriter<RevFeature> writer = factory.<RevFeature> createObjectWriter(TYPE.FEATURE);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writer.write(newFeature, output);

        byte[] data = output.toByteArray();
        assertTrue(data.length > 0);

        ObjectReader<RevFeature> reader = factory.<RevFeature> createObjectReader(TYPE.FEATURE);
        ByteArrayInputStream input = new ByteArrayInputStream(data);
        RevFeature feat = reader.read(newFeature.getId(), input);

        assertNotNull(feat);
        assertEquals(newFeature.getValues().size(), feat.getValues().size());

        for (int i = 0; i < newFeature.getValues().size(); i++) {
            assertEquals(newFeature.getValues().get(i).orNull(), feat.getValues().get(i).orNull());
        }

    }

    protected Feature feature(Schema type, String id, Object... values)
            throws ParseException {
        List<Object> list = Lists.newArrayList(); 
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (type.getFields().get(i).isGeometry()) {
                if (value instanceof String) {
                    value = new WKTReader().read((String) value);
                }
            }
            list.add(value);
        }
        return new BasicFeature(id, list, type);
    }
}
