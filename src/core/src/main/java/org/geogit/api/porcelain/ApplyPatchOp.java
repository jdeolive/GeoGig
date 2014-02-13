/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.FeatureInfo;
import org.geogit.api.NodeRef;
import org.geogit.api.Ref;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.diff.AttributeDiff;
import org.geogit.api.plumbing.diff.AttributeDiff.TYPE;
import org.geogit.api.plumbing.diff.FeatureDiff;
import org.geogit.api.plumbing.diff.FeatureTypeDiff;
import org.geogit.api.plumbing.diff.Patch;
import org.geogit.api.plumbing.diff.VerifyPatchOp;
import org.geogit.api.plumbing.diff.VerifyPatchResults;
import org.geogit.repository.DepthSearch;
import org.geogit.repository.WorkingTree;
import org.geogit.storage.StagingDatabase;
import org.jeo.feature.BasicFeature;
import org.jeo.feature.Feature;
import org.jeo.feature.Field;
import org.jeo.feature.Schema;
import org.jeo.feature.SchemaBuilder;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

/**
 * Applies a patch to the working tree. If partial application of the patch is allowed, it returns a
 * patch with the elements that could not be applied (might be an empty patch), or null otherwise
 * 
 * @see WorkingTree
 * @see Patch
 */
public class ApplyPatchOp extends AbstractGeoGitOp<Patch> {

    private Patch patch;

    private WorkingTree workTree;

    private boolean applyPartial;

    private StagingDatabase indexDb;

    private boolean reverse;

    /**
     * Constructs a new {@code ApplyPatchOp} with the given parameters.
     * 
     * @param workTree the working tree to modify when applying the patch
     */
    @Inject
    public ApplyPatchOp(final WorkingTree workTree, StagingDatabase indexDb) {
        this.workTree = workTree;
        this.indexDb = indexDb;
    }

    /**
     * Sets the patch to apply
     * 
     * @param patch the patch to apply
     * @return {@code this}
     */
    public ApplyPatchOp setPatch(Patch patch) {
        this.patch = patch;
        return this;
    }

    /**
     * Sets whether to apply the original patch or its reversed version
     * 
     * @param reverse true if the patch should be applied in its reversed version
     * @return {@code this}
     */
    public ApplyPatchOp setReverse(boolean reverse) {
        this.reverse = reverse;
        return this;
    }

    /**
     * Sets whether the patch can be applied partially or not
     * 
     * @param applyPartial whether the patch can be applied partially or not
     * @return {@code this}
     */
    public ApplyPatchOp setApplyPartial(boolean applyPartial) {
        this.applyPartial = applyPartial;
        return this;
    }

    /**
     * Sets whether to use the index instead of the working tree.
     * 
     * TODO: This option is currently unused
     * 
     * @param cached whether to use the index instead of the working tree.
     * @return {@code this}
     */
    public ApplyPatchOp setCached(boolean cached) {
        // this.cached = cached;
        return this;
    }

    /**
     * Executes the apply command, applying the given patch If it cannot be applied and no partial
     * application is allowed, a {@link CannotApplyPatchException} exception is thrown. Returns a
     * patch with rejected entries, in case partial application is allowed
     * 
     * @return the modified {@link WorkingTree working tree}.
     */
    @Override
    public Patch call() throws RuntimeException {
        Preconditions.checkArgument(patch != null, "No patch file provided");

        VerifyPatchResults verify = command(VerifyPatchOp.class).setPatch(patch)
                .setReverse(reverse).call();
        Patch toReject = verify.getToReject();
        Patch toApply = verify.getToApply();

        if (!applyPartial) {
            if (!toReject.isEmpty()) {
                throw new CannotApplyPatchException(toReject);
            }
            applyPatch(toApply);
            return null;

        } else {
            applyPatch(toApply);
            return toReject;
        }

    }

    private void applyPatch(Patch patch) {

        if (reverse) {
            patch = patch.reversed();
        }

        List<FeatureInfo> removed = patch.getRemovedFeatures();
        for (FeatureInfo feature : removed) {
            workTree.delete(NodeRef.parentPath(feature.getPath()),
                    NodeRef.nodeFromPath(feature.getPath()));
        }
        List<FeatureInfo> added = patch.getAddedFeatures();
        for (FeatureInfo feature : added) {
            workTree.insert(NodeRef.parentPath(feature.getPath()), feature.getFeature());
        }
        List<FeatureDiff> diffs = patch.getModifiedFeatures();
        for (FeatureDiff diff : diffs) {
            String path = diff.getPath();
            DepthSearch depthSearch = new DepthSearch(indexDb);
            Optional<NodeRef> noderef = depthSearch.find(workTree.getTree(), path);
            RevFeatureType oldRevFeatureType = command(RevObjectParse.class)
                    .setObjectId(noderef.get().getMetadataId()).call(RevFeatureType.class).get();
            String refSpec = Ref.WORK_HEAD + ":" + path;
            RevFeature feature = command(RevObjectParse.class).setRefSpec(refSpec)
                    .call(RevFeature.class).get();

            RevFeatureType newRevFeatureType = getFeatureType(diff, feature, oldRevFeatureType);
            ImmutableList<Optional<Object>> values = feature.getValues();
            ImmutableList<Field> oldDescriptors = oldRevFeatureType
                    .sortedDescriptors();
            ImmutableList<Field> newDescriptors = newRevFeatureType
                    .sortedDescriptors();
 
            Map<String, Optional<?>> attrs = Maps.newHashMap();
            for (int i = 0; i < oldDescriptors.size(); i++) {
                Field descriptor = oldDescriptors.get(i);
                if (newDescriptors.contains(descriptor)) {
                    Optional<Object> value = values.get(i);
                    attrs.put(descriptor.getName(), value);
                }
            }
            Set<Entry<Field, AttributeDiff>> featureDiffs = diff.getDiffs().entrySet();
            for (Iterator<Entry<Field, AttributeDiff>> iterator = featureDiffs
                    .iterator(); iterator.hasNext();) {
                Entry<Field, AttributeDiff> entry = iterator.next();
                if (!entry.getValue().getType().equals(TYPE.REMOVED)) {
                    Optional<?> oldValue = attrs.get(entry.getKey().getName());
                    attrs.put(entry.getKey().getName(), entry.getValue().applyOn(oldValue));
                }
            }

            Map<String,Object> map = Maps.transformValues(attrs, new Function<Optional<?>,Object>() {
                @Override
                public Object apply(Optional<?> opt) {
                    return opt.orNull();
                }
            });

            Feature featureToInsert = 
                new BasicFeature(NodeRef.nodeFromPath(path), map, newRevFeatureType.type());
            workTree.insert(NodeRef.parentPath(path), featureToInsert);

        }
        
        ImmutableList<FeatureTypeDiff> alteredTrees = patch.getAlteredTrees();
        for (FeatureTypeDiff diff : alteredTrees) {
            Optional<RevFeatureType> featureType;
            if (diff.getOldFeatureType().isNull()) {
                featureType = patch.getFeatureTypeFromId(diff.getNewFeatureType());
                workTree.createTypeTree(diff.getPath(), featureType.get().type());
            } else if (diff.getNewFeatureType().isNull()) {
                workTree.delete(diff.getPath());
            } else {
                featureType = patch.getFeatureTypeFromId(diff.getNewFeatureType());
                workTree.updateTypeTree(diff.getPath(), featureType.get().type());
            }
        }

    }

    private RevFeatureType getFeatureType(FeatureDiff diff, RevFeature oldFeature,
            RevFeatureType oldRevFeatureType) {
        List<String> removed = Lists.newArrayList();
        List<Field> added = Lists.newArrayList();

        Set<Entry<Field, AttributeDiff>> featureDiffs = diff.getDiffs().entrySet();
        for (Iterator<Entry<Field, AttributeDiff>> iterator = featureDiffs.iterator(); iterator
                .hasNext();) {
            Entry<Field, AttributeDiff> entry = iterator.next();
            if (entry.getValue().getType() == TYPE.REMOVED) {
                removed.add(entry.getKey().getName());
            } else if (entry.getValue().getType() == TYPE.ADDED) {
                Field pd = entry.getKey();
                added.add((Field) pd);
            }
        }

        Schema sft = (Schema) oldRevFeatureType.type();
        List<Field> descriptors = (sft).getFields();

        SchemaBuilder  featureTypeBuilder = new SchemaBuilder(sft.getName());

        for (int i = 0; i < descriptors.size(); i++) {
            Field descriptor = descriptors.get(i);
            if (!removed.contains(descriptor.getName())) {
                featureTypeBuilder.field(descriptor);
            }
        }
        for (Field descriptor : added) {
            featureTypeBuilder.field(descriptor);
        }
        Schema featureType = featureTypeBuilder.schema();
        return RevFeatureType.build(featureType);
    }

}
