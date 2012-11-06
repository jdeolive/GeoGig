/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.api.RevObject;
import org.geogit.api.RevObject.TYPE;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.StagingDatabase;

import com.google.common.base.Optional;
import com.google.inject.Inject;

/**
 * Resolves the reference given by a ref spec to the {@link RevObject} it finally points to,
 * dereferencing symbolic refs as necessary.
 * 
 * @see RevParse
 * @see ResolveObjectType
 */
public class RevObjectParse extends AbstractGeoGitOp<Optional<RevObject>> {

    private StagingDatabase indexDb;

    private ObjectSerialisingFactory serialFactory;

    private ObjectId objectId;

    private String refSpec;

    /**
     * Constructs a new {@class RevObjectParse} operation with the given parameters.
     * 
     * @param indexDb the staging database
     * @param serialFactory the serialization factory
     */
    @Inject
    public RevObjectParse(StagingDatabase indexDb, ObjectSerialisingFactory serialFactory) {
        this.indexDb = indexDb;
        this.serialFactory = serialFactory;
    }

    /**
     * @param refSpec the ref spec to resolve
     * @return this
     */
    public RevObjectParse setRefSpec(final String refSpec) {
        this.objectId = null;
        this.refSpec = refSpec;
        return this;
    }

    /**
     * @param objectId the {@link ObjectId object id} to resolve
     * @return this
     */
    public RevObjectParse setObjectId(final ObjectId objectId) {
        this.refSpec = null;
        this.objectId = objectId;
        return this;
    }

    /**
     * @return the resolved object id
     * @throws IllegalArgumentException if the provided refspec doesn't resolve to any known object
     * @see RevObject
     */
    @Override
    public Optional<RevObject> call() throws IllegalArgumentException {
        return call(RevObject.class);
    }

    /**
     * @param clazz the base type of the parsed objects
     * @return the resolved object id
     * @see RevObject
     */
    public <T extends RevObject> Optional<T> call(Class<T> clazz) {
        final ObjectId resolvedObjectId;
        if (objectId == null) {
            Optional<ObjectId> parsed = command(RevParse.class).setRefSpec(refSpec).call();
            if (parsed.isPresent()) {
                resolvedObjectId = parsed.get();
            } else {
                resolvedObjectId = ObjectId.NULL;
            }
        } else {
            resolvedObjectId = objectId;
        }
        if (resolvedObjectId.isNull()) {
            return Optional.absent();
        }

        final TYPE type = command(ResolveObjectType.class).setObjectId(resolvedObjectId).call();
        ObjectReader<? extends RevObject> reader;
        switch (type) {
        case FEATURE:
            throw new UnsupportedOperationException("not yet implemented");
            // break;
        case COMMIT:
            reader = serialFactory.createCommitReader();
            break;
        case TAG:
            throw new UnsupportedOperationException("not yet implemented");
            // break;
        case TREE:
            reader = serialFactory.createRevTreeReader(indexDb);
            break;
        case FEATURETYPE:
            reader = serialFactory.createFeatureTypeReader();
            break;
        default:
            throw new IllegalArgumentException("Unknown object type " + type);
        }

        RevObject revObject = indexDb.get(resolvedObjectId, reader);
        return Optional.of(clazz.cast(revObject));
    }
}
