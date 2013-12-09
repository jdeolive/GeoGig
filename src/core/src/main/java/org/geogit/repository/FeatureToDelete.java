package org.geogit.repository;

import java.util.Collection;
import java.util.Map;

import org.geotools.filter.identity.FeatureIdImpl;
import org.opengis.feature.Feature;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.Property;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.identity.FeatureId;
import org.opengis.geometry.BoundingBox;

/**
 * An object representing a feature to be deleted. When this is inserted into the working tree of a
 * repository, the feature with the specified path and name will be deleted instead
 * 
 */
public class FeatureToDelete implements Feature {

    private FeatureId fid;

    private String path;

    /**
     * Constructs a new feature to be deleted
     * 
     * @param path the path to the feature
     * @param name the name of the feature
     * 
     */
    public FeatureToDelete(String path, String name) {
        fid = new FeatureIdImpl(name);
        this.path = path;
    }

    @Override
    public Collection<Property> getProperties() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Property> getProperties(Name arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Property> getProperties(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property getProperty(Name arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property getProperty(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<? extends Property> getValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setValue(Collection<Property> arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void validate() throws IllegalAttributeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public AttributeDescriptor getDescriptor() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Name getName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<Object, Object> getUserData() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isNillable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setValue(Object arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BoundingBox getBounds() {
        throw new UnsupportedOperationException();
    }

    @Override
    public GeometryAttribute getDefaultGeometryProperty() {
        throw new UnsupportedOperationException();
    }

    @Override
    public FeatureId getIdentifier() {
        return fid;
    }

    @Override
    public FeatureType getType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDefaultGeometryProperty(GeometryAttribute arg0) {
        throw new UnsupportedOperationException();
    }

    public String getPath() {
        return path;
    }

}
