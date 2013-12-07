/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject.TYPE;
import org.jeo.feature.Schema;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public abstract class RevFeatureTypeSerializationTest extends Assert {
    private ObjectSerializingFactory factory = getObjectSerializingFactory();
    private String namespace = "http://geoserver.org/test";
    private String typeName = "TestType";
    private String typeSpec = "str:String," + "bool:Boolean," + "byte:java.lang.Byte,"
                + "doub:Double," + "bdec:java.math.BigDecimal," + "flt:Float," + "int:Integer,"
                + "bint:java.math.BigInteger," + "pp:Point:srid=4326," + "lng:java.lang.Long,"
                + "uuid:java.util.UUID";
    private Schema featureType;
    protected abstract ObjectSerializingFactory getObjectSerializingFactory();

    @Before
    public void setUp() throws Exception {
        featureType = Schema.build(typeName).uri(namespace).fields(typeSpec).schema();
    }
    
    @Test
    public void testSerialization() throws Exception {
        RevFeatureType revFeatureType = RevFeatureType.build(featureType);
        ObjectWriter<RevFeatureType> writer = factory.createObjectWriter(TYPE.FEATURETYPE);
    
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writer.write(revFeatureType, output);
    
        byte[] data = output.toByteArray();
        assertTrue(data.length > 0);
    
        ObjectReader<RevFeatureType> reader = factory.createObjectReader(TYPE.FEATURETYPE);
        ByteArrayInputStream input = new ByteArrayInputStream(data);
        RevFeatureType rft = reader.read(revFeatureType.getId(), input);
    
        assertNotNull(rft);
        Schema serializedFeatureType = rft.type();
        assertEquals(serializedFeatureType.getFields().size(), featureType.getFields().size());
    
        for (int i = 0; i < featureType.getFields().size(); i++) {
            assertEquals(featureType.getFields().get(i), serializedFeatureType.getFields().get(i));
        }
    
        assertEquals(featureType.geometry(), serializedFeatureType.geometry());
        assertEquals(featureType.crs(), serializedFeatureType.crs());
    }
}
