package org.geogit.repository;

import java.util.List;
import java.util.Map;

import org.jeo.feature.Feature;
import org.jeo.feature.Schema;
import org.osgeo.proj4j.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

/**
 * An object representing a feature to be deleted. When this is inserted into the working tree of a
 * repository, the feature with the specified path and name will be deleted instead
 * 
 */
public class FeatureToDelete implements Feature {

    private String fid;

    private Schema type;

    /**
     * Constructs a new feature to be deleted
     * 
     * @param ft the path to the feature
     * @param name the name of the feature
     * 
     */
    public FeatureToDelete(Schema ft, String name) {
        this.fid = name;
        this.type = ft;
    }

    @Override
    public String getId() {
        return fid;
    }

    @Override
    public Schema schema() {
        return type;
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
    public CoordinateReferenceSystem getCRS() {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
    public void setCRS(CoordinateReferenceSystem crs) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
    public CoordinateReferenceSystem crs() {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
    public Object get(String key) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
    public Object get(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
    public Geometry geometry() {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
    public void put(String key, Object val) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
    public void set(int index, Object val) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
    public void put(Geometry g) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
    public List<Object> list() {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
    public Map<String, Object> map() {
        throw new UnsupportedOperationException();
    }
}
