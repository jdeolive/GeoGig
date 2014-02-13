/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.storage;

import java.io.Serializable;

import org.geogit.api.Node;
import org.geogit.api.ObjectId;
import org.geogit.api.RevTree;

import com.google.common.base.Preconditions;
import com.google.common.collect.Ordering;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;

/**
 * Implements storage order of {@link Node} based on the non cryptographic 64-bit <a
 * href="http://en.wikipedia.org/wiki/Fowler%E2%80%93Noll%E2%80%93Vo_hash_function">FNV-1a</a>
 * variation of the "Fowler/Noll/Vo" hash algorithm.
 * <p>
 * This class mandates in which order {@link Node nodes} are stored inside {@link RevTree trees},
 * hence defining the prescribed order in which tree elements are traversed, regardless of in how
 * many subtrees a given tree is split into.
 * <p>
 * The resulting structure where a given node (identified by its name) always falls on the same
 * bucket (subtree) for a given subtree depth makes it possible to compute diffs between two trees
 * very quickly, by traversing both trees in parallel and finding both bucket and node differences
 * and skipping equal bucket trees, as two buckets at the same depth with the same contents will
 * always hash out to the same {@link ObjectId}.
 * <p>
 * The FNV-1 hash for a node name is computed as in the following pseudo-code:
 * 
 * <pre>
 * <code>
 * hash = FNV_offset_basis
 * for each octet_of_data to be hashed
 *      hash = hash × FNV_prime
 *      hash = hash XOR octet_of_data
 * return hash
 * </code>
 * </pre>
 * 
 * Where {@code FNV_offset_basis} is the 64-bit literal {@code 0xcbf29ce484222325}, and
 * {@code FNV_prime} is the 64-bit literal {@code 0x100000001b3}.
 * <p>
 * To compute the node name hash, each two-byte char in the node name produces two
 * {@code octet_of_data}, in big-endian order.
 * <p>
 * This hash function proved to be extremely fast while maintaining a good distribution and low
 * collision rate, and is widely used by the computer science industry as explained <a
 * href="http://www.isthe.com/chongo/tech/comp/fnv/index.html#FNV-param">here</a> when speed is
 * needed in contrast to cryptographic security.
 * 
 * @since 0.6
 */
public final class NodePathStorageOrder extends Ordering<String> implements Serializable {

    private static final long serialVersionUID = -685759544293388523L;

    private static final HashOrder hashOrder = new FNV1a64bitHash();

    @Override
    public int compare(String p1, String p2) {
        return hashOrder.compare(p1, p2);
    }

    /**
     * Computes the bucket index that corresponds to the given node name at the given depth.
     * 
     * @return and Integer between zero and {@link RevTree#MAX_BUCKETS} minus one
     */
    public Integer bucket(final String nodeName, final int depth) {

        final int byteN = hashOrder.byteN(nodeName, depth);
        Preconditions.checkState(byteN >= 0);
        Preconditions.checkState(byteN < 256);

        final int maxBuckets = RevTree.MAX_BUCKETS;

        final int bucket = (byteN * maxBuckets) / 256;
        return Integer.valueOf(bucket);
    }

    private static abstract class HashOrder extends Ordering<String> implements Serializable {

        private static final long serialVersionUID = -469599567110937126L;

        public abstract int byteN(String path, int depth);

    }

    /**
     * The FNV-1a hash function used as {@link Node} storage order.
     * <p>
     * Note this function does not strictly produce a FNV-1a hash code but its absolute value.
     * Reason is to avoid negative values, so the sign bit does not mess up the
     * {@link #byteN(String, int)} function which returns the byte value of one of the 8 bytes from
     * the hash code to assign a bucket
     */
    private static class FNV1a64bitHash extends HashOrder {

        private static final long serialVersionUID = -1931193743208260766L;

        private static final long FNV64_OFFSET_BASIS = 0xcbf29ce484222325L;

        private static final long FNV64_PRIME = 0x100000001b3L;

        @Override
        public int compare(String p1, String p2) {
            long hash1 = fnvAbs(p1);
            long hash2 = fnvAbs(p2);
            if (hash1 == hash2) {
                // hash collision, fallback to lexicographic order.
                // unlikely but you never know. Haven't have a collision with 50 million nodes
                return p1.compareTo(p2);
            }
            return hash1 < hash2 ? -1 : 1;
        }

        private static long fnv(CharSequence chars) {
            final int length = chars.length();

            long hash = FNV64_OFFSET_BASIS;
            Preconditions.checkState(hash < Long.MAX_VALUE);

            for (int i = 0; i < length; i++) {
                char c = chars.charAt(i);
                byte b1 = (byte) (c >> 8);
                byte b2 = (byte) c;
                hash = update(hash, b1);
                hash = update(hash, b2);
            }
            return hash;
        }

        /**
         * @return the absolute value of the fnv-1a hash for the given char sequence
         */
        public static long fnvAbs(CharSequence chars) {
            return Math.abs(fnv(chars));
        }

        private static long update(long hash, byte octet) {
            hash = hash ^ octet;
            hash = hash * FNV64_PRIME;
            return hash;
        }

        /**
         * Computes the bucket index that corresponds to the given node name at the given depth.
         * 
         * @return and Integer between zero and {@link RevTree#MAX_BUCKETS} minus one
         */
        @Override
        public int byteN(final String nodeName, final int depth) {

            final long hashCode = fnvAbs(nodeName);
            switch (depth) {
            case 0:
                return (int) (hashCode >>> 56) & 0xFF;
            case 1:
                return (int) (hashCode >>> 48) & 0xFF;
            case 2:
                return (int) (hashCode >>> 40) & 0xFF;
            case 3:
                return (int) (hashCode >>> 32) & 0xFF;
            case 4:
                return (int) (hashCode >>> 24) & 0xFF;
            case 5:
                return (int) (hashCode >>> 16) & 0xFF;
            case 6:
                return (int) (hashCode >>> 8) & 0xFF;
            case 7:
                return (int) (hashCode >>> 0) & 0xFF;
            default:
                throw new IllegalArgumentException("depth too deep: " + depth);
            }
        }

    }

    /**
     * A HashOrder based on a {@link HashFunction}, for testing purposes, used to be the default
     * with a SHA-1 hash algorithm before 0.6.0
     */
    private static class HashFunctionOrder extends HashOrder {

        private static final long serialVersionUID = 1266787641620204350L;

        private final HashFunction hasher;

        public HashFunctionOrder(HashFunction hasher) {
            this.hasher = hasher;
        }

        @Override
        public int compare(String p1, String p2) {
            HashCode h1 = hasher.hashString(p1);
            HashCode h2 = hasher.hashString(p2);
            return ObjectId.compare(h1.asBytes(), h2.asBytes());
        }

        /**
         * Computes the bucket index that corresponds to the given node name at the given depth.
         * 
         * @return and Integer between zero and {@link RevTree#MAX_BUCKETS} minus one
         */
        @Override
        public int byteN(final String nodeName, final int depth) {
            byte[] hashCode = hasher.hashString(nodeName).asBytes();
            final int byteN = (byte) hashCode[depth] & 0xFF;
            return byteN;
        }
    }

    // public static void main(String a[]) {
    // try {
    // final int c = 5 * 1000 * 1000;
    //
    // List<Node> nodes = new ArrayList<Node>(c);
    // for (int i = 0; i < c; i++) {
    // Node node = node(i);
    // nodes.add(node);
    // }
    // nodes = Collections.unmodifiableList(nodes);
    //
    // System.err.println("\nTesting FNV1-A hash");
    // NodePathStorageOrder.hashOrder = new FNV1a64bitHash();
    // test(c, nodes);
    //
    // System.err.println("\nTesting murmur3_32 hash");
    // NodePathStorageOrder.hashOrder = new HashFunctionOrder(Hashing.murmur3_32());
    // test(c, nodes);
    //
    // System.err.println("\nTesting SHA-1 hash");
    // NodePathStorageOrder.hashOrder = new HashFunctionOrder(Hashing.sha1());
    // test(c, nodes);
    // } finally {
    // System.exit(0);
    // }
    // }
    //
    // private static void test(int c, List<Node> nodes) {
    // ObjectDatabase db = new HeapObjectDatabse(new DataStreamSerializationFactory());
    // db.open();
    //
    // RevTreeBuilder builder = new RevTreeBuilder(db);
    // nodes = new ArrayList<Node>(nodes);
    // System.err.print("sorting... ");
    // Stopwatch s = new Stopwatch().start();
    // Collections.sort(nodes, new NodeStorageOrder());
    // System.err.printf("%,d nodes sorted in %s\n", nodes.size(), s.stop());
    //
    // s.reset().start();
    // for (Node node : nodes) {
    // builder.put(node);
    // }
    // RevTree tree = builder.build();
    // db.put(tree);
    // System.err.printf("%,d nodes tree created in %s\n", tree.size(), s.stop());
    //
    // Set<Node> set = new TreeSet<Node>(new NodeStorageOrder());
    // set.addAll(nodes);
    // int diff = nodes.size() - set.size();
    // System.err.printf("%,d collisions\n", diff);
    // print(tree, db);
    // db.close();
    // }
    //
    // private static void print(RevTree tree, ObjectDatabase db) {
    // Iterator<RevTree> it = new DeepMove.AllTrees(tree.getId(), db);
    // int n = Iterators.size(it);
    // System.err.printf("%,d trees\n", n);
    //
    // Iterator<NodeRef> refs = new DepthTreeIterator("", ObjectId.NULL, tree, db,
    // Strategy.FEATURES_ONLY);
    // Stopwatch s = new Stopwatch().start();
    // n = Iterators.size(refs);
    // System.err.printf("%,d features traversed in %s\n", n, s.stop());
    // }
    //
    // private static Node node(int i) {
    // String name = String.valueOf(i);
    // return Node.create(name, ObjectId.forString(name), ObjectId.NULL, TYPE.FEATURE);
    // }
}