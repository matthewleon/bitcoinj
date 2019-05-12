package org.bitcoinj.core;

import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.UnsignedLongs;
import com.tomgibara.bits.BitReader;
import com.tomgibara.bits.BitWriter;
import com.tomgibara.bits.Bits;
import com.tomgibara.streams.StreamBytes;
import com.tomgibara.streams.Streams;
import com.tomgibara.streams.WriteStream;
import org.bouncycastle.crypto.macs.SipHash;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.util.Pack;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

import static com.google.common.primitives.Bytes.concat;
import static org.bitcoinj.script.ScriptOpCodes.OP_RETURN;

public final class GolombCodedSet {

    private static final long LOWER_32_MASK = 0xffffffffL;

    private static final Comparator<Long> UNSIGNED_LONG_COMPARATOR = new Comparator<Long>() {
        @Override
        public int compare(Long o1, Long o2) {
            return UnsignedLongs.compare(o1, o2);
        }
    };
    
    private final long n;
    
    private final byte[] compressedSet;
    
    private GolombCodedSet(long n, byte[] compressedSet) {
        this.n = n;
        this.compressedSet = compressedSet;
    }

    public static GolombCodedSet deserialize(byte[] serialized) {
        VarInt n = new VarInt(serialized, 0);
        byte[] compressedSet = Arrays.copyOfRange(serialized, n.getOriginalSizeInBytes(), serialized.length);
        return new GolombCodedSet(n.value, compressedSet);
    }
    
    public static GolombCodedSet buildBip158(Block block, Iterable<byte[]> previousOutputScripts) {
        final int bip158p = 19;
        final int bip158m = 784931;
        
        byte[] blockHashLittleEndian = block.getHash().getReversedBytes();
        KeyParameter k = new KeyParameter(blockHashLittleEndian, 0, 16);
        ImmutableSet<Bytes> rawItems = getFilterElements(block, previousOutputScripts);
        
        return build(rawItems, bip158p, k, bip158m);
    }

    private static ImmutableSet<Bytes> getFilterElements(Block block, Iterable<byte[]> previousOutputScripts) {
        ImmutableSet.Builder<Bytes> rawItemsBuilder = ImmutableSet.builder();
        for (byte[] previousOutputScript : previousOutputScripts) {
            if (previousOutputScript.length > 0)
                rawItemsBuilder.add(new Bytes(previousOutputScript));
        }

        List<Transaction> transactions = block.getTransactions();
        if (transactions != null) {
            for (Transaction t : transactions) {
                for (TransactionOutput to : t.getOutputs()) {
                    byte[] script = to.getScriptBytes();
                    if (script.length > 1 && script[0] != (byte) OP_RETURN)
                        rawItemsBuilder.add(new Bytes(script));
                }
            }
        }

        return rawItemsBuilder.build();
    }
    
    public static GolombCodedSet build(Set<Bytes> rawItems, int p, KeyParameter k, long m) {
        SortedSet<Long> items = hashedSetConstruct(rawItems, k, m);
        StreamBytes streamBytes = Streams.bytes();
        try (WriteStream writeStream = streamBytes.writeStream()) {
            BitWriter writer = Bits.writerTo(writeStream);
            long last_value = 0;
            for (Long item : items) {
                long delta = item - last_value;
                golombEncode(writer, delta, p);
                last_value = item;
            }
            writer.flush();
        }
        long n = ((long) items.size() & LOWER_32_MASK);
        byte[] compressedSet = streamBytes.bytes();
        return new GolombCodedSet(n, compressedSet);
    }
    
    public long size() {
        return n;
    }
    
    public byte[] serialize() {
        byte[] size = new VarInt(n).encode();
        return concat(size, compressedSet);
    }

    boolean bip158match(KeyParameter k, byte[] target) {
        final int bip158p = 19;
        final int bip158m = 784931;

        return match(k, target, bip158p, bip158m);
    }

    boolean match(KeyParameter k, byte[] target, int p, long m) {
        long f = n * m;
        long targetHash = hashToRange(target, f, k);


        BitReader reader = Bits.readerFrom(compressedSet);
        long lastValue = 0;
        for (long i = 0; i < n; i++) {
            long delta = golombDecode(reader, p);
            lastValue += delta;
            if (lastValue == targetHash)
                return true;
            if (lastValue > targetHash)
                break;
        }

        return false;
    }
    
    private static SortedSet<Long> hashedSetConstruct(Set<Bytes> rawItems, KeyParameter k, long m) {
        long n = ((long) rawItems.size()) & LOWER_32_MASK;
        long f = n * m;
        
        SortedSet<Long> out = new ConcurrentSkipListSet<>(UNSIGNED_LONG_COMPARATOR);
        for (Bytes rawItem : rawItems) {
            out.add(hashToRange(rawItem.bytes, f, k));
        }
        
        return out;
    }

    public static void golombEncode(BitWriter writer, long x, int p) {
        writer.writeBooleans(true, x >>> p);
        writer.writeBit(0);
        writer.write(x, p);
    }
    
    static long golombDecode(BitReader reader, int p) {
        int q = reader.readUntil(false);

        long r = reader.readLong(p);
        return ((long) q << p) + r;
    }

    private static long hashToRange(byte[] bytes, long f, KeyParameter k) {
        long hash = sipHashBigEndian(bytes, k);
        return mapIntoRange(hash, f);
    }

    private static long sipHashBigEndian(byte[] item, KeyParameter k) {
        // the SipHash provided with bouncycastle operates on Little Endian data
        byte[] hashLittleEndian = new byte[8];
        SipHash sipHash = new SipHash(2, 4);
        sipHash.init(k);
        sipHash.update(item, 0, item.length);
        sipHash.doFinal(hashLittleEndian, 0);
        return Pack.littleEndianToLong(hashLittleEndian, 0);
    }

    /**
     * Map a value x that is uniformly distributed in the range [0, 2^64) to a
     * value uniformly distributed in [0, n) by returning the upper 64 bits of
     * by returning the upper 64 bit of x * n.
     *
     * The code here is ported from Bitcoin core's blockfilter.cpp, which in turn borrows
     * from a StackOverflow answer:
     * See: https://stackoverflow.com/a/26855440
     *
     */
    private static long mapIntoRange(long x, long n) {
        final long a = x >>> 32;
        final long b = x & LOWER_32_MASK;
        final long c = n >>> 32;
        final long d = n & LOWER_32_MASK;

        final long ac = a * c;
        final long ad = a * d;
        final long bc = b * c;
        final long bd = b * d;

        final long mid34 = (bd >>> 32) + (bc & LOWER_32_MASK) + (ad & LOWER_32_MASK);
        return ac + (bc >>> 32) + (ad >>> 32) + (mid34 >>> 32);
    }

    private static final class Bytes {
        private final byte[] bytes;

        private Bytes(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Bytes other = (Bytes) o;
            return Arrays.equals(this.bytes, other.bytes);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(bytes);
        }
    }
    
}
