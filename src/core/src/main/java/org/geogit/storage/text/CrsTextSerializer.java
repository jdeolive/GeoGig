/* Copyright (c) 2013 OpenPlans. All rights reserved. 
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.text;

import org.jeo.proj.Proj;
import org.osgeo.proj4j.CoordinateReferenceSystem;

public class CrsTextSerializer {

    public static String serialize(CoordinateReferenceSystem crs) {
        String srsName;
        if (crs == null) {
            srsName = "urn:ogc:def:crs:EPSG::0";
        } else {
            // use a flag to control whether the code is returned in EPSG: form instead of
            // urn:ogc:.. form irrespective of the org.geotools.referencing.forceXY System
            // property.
            final boolean longitudeFirst = true;
            boolean codeOnly = true;
            Integer crsCode = Proj.epsgCode(crs);
            if (crsCode != null) {
                srsName = (longitudeFirst ? "EPSG:" : "urn:ogc:def:crs:EPSG::") + crsCode;
                // check that what we are writing is actually a valid EPSG code and we will
                // be able to decode it later. If not, we will use WKT instead
                try {
                    Proj.crs(srsName);
                } catch (Exception e) {
                    srsName = null;
                } 
            } else {
                srsName = null;
            }
        }
        if (srsName == null) {
            srsName = crs.getParameterString();
        }
        return srsName;
    }

    public static CoordinateReferenceSystem deserialize(String crsText) {
        CoordinateReferenceSystem crs;
        boolean crsCode = crsText.startsWith("EPSG")
                || crsText.startsWith("urn:ogc:def:crs:EPSG");
        try {
            if (crsCode) {
                if ("urn:ogc:def:crs:EPSG::0".equals(crsText)) {
                    crs = null;
                } else {
                    crs = Proj.crs(crsText);
                }
            } else {
                crs = Proj.crs(crsText);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot parse CRS definition: " + crsText);
        }

        return crs;
    }

}
