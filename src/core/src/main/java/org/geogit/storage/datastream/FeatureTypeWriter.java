/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.datastream;

import static org.geogit.storage.datastream.FormatCommon.writeHeader;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.geogit.api.RevFeatureType;
import org.geogit.storage.FieldType;
import org.geogit.storage.ObjectWriter;
import org.jeo.feature.Field;
import org.jeo.proj.Proj;
import org.opengis.feature.type.Name;
import org.osgeo.proj4j.CoordinateReferenceSystem;

public class FeatureTypeWriter implements ObjectWriter<RevFeatureType> {
    @Override
    public void write(RevFeatureType object, OutputStream out) throws IOException {
        DataOutput data = new DataOutputStream(out);
        writeHeader(data, "featuretype");
        writeName(object.getName(), data);
        data.writeInt(object.sortedDescriptors().size());
        for (Field desc : object.type()) {
            writeProperty(desc, data);
        }
    }

    private void writeName(String name, DataOutput data) throws IOException {
        writeName(new Name(name), data);
    }

    private void writeName(Name name, DataOutput data) throws IOException {
        final String ns = name.getNamespaceURI();
        final String lp = name.getLocalPart();
        data.writeUTF(ns == null ? "" : ns);
        data.writeUTF(lp == null ? "" : lp);
    }

    private void writePropertyType(Field type, DataOutput data) throws IOException {
        writeName(type.getName(), data);
        data.writeByte(FieldType.forBinding(type.getType()).getTag());
        if (type.isGeometry()) {
            CoordinateReferenceSystem crs = type.getCRS();
            String srsName;
            if (crs == null) {
                srsName = "urn:ogc:def:crs:EPSG::0";
            } else {
                final boolean longitudeFirst = true;
                final boolean codeOnly = true;
                Integer crsCode = Proj.epsgCode(crs);
                if (crsCode != null) {
                    srsName = (longitudeFirst ? "EPSG:" : "urn:ogc:def:crs:EPSG::") + crsCode;
                    // check that what we are writing is actually a valid EPSG code and we will be
                    // able to decode it later. If not, we will use WKT instead
                    try {
                        if (Proj.crs(srsName) == null) {
                            srsName = null;
                        }
                    } catch (Exception e) {
                        srsName = null;
                    }
                } else {
                    srsName = null;
                }
            }
            if (srsName != null) {
                data.writeBoolean(true);
                data.writeUTF(srsName);
            } else {
                data.writeBoolean(false);
                data.writeUTF(crs.getParameterString());
            }
        }
    }

    private void writeProperty(Field attr, DataOutput data) throws IOException {
        writeName(attr.getName(), data);
        //TODO: add properties to Field?
        data.writeBoolean(true);
        data.writeInt(1);
        data.writeInt(1);
        writePropertyType(attr, data);
    }
}
