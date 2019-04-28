package org.bitcoinj.core;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.tomgibara.bits.BitReader;
import com.tomgibara.bits.BitWriter;
import com.tomgibara.bits.Bits;
import com.tomgibara.streams.StreamBytes;
import com.tomgibara.streams.Streams;
import com.tomgibara.streams.WriteStream;
import org.bitcoinj.script.Script;
import org.bouncycastle.crypto.macs.SipHash;
import org.bouncycastle.crypto.params.KeyParameter;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

public class GolombCodedSet {
    
    private static final Comparator<Long> UNSIGNED_LONG_COMPARATOR = new Comparator<Long>() {
        @Override
        public int compare(Long o1, Long o2) {
            return (int) (o1 - o2);
        }
    };
    
    private final int n;
    
    private final byte[] compressedSet;
    
    private GolombCodedSet(int n, byte[] compressedSet) {
        this.n = n;
        this.compressedSet = compressedSet;
    }
    
    public static GolombCodedSet buildBip158(Block block, Iterable<byte[]> previousOutputScripts) {
        final int bip158p = 19;
        final int bip158m = 784931;
        
        KeyParameter k = new KeyParameter(block.getHash().getBytes(), 0, 16);
        
        ImmutableList.Builder<byte[]> rawItemsBuilder = new ImmutableList.Builder<>();
        rawItemsBuilder.addAll(previousOutputScripts);
        for (Transaction t : block.getTransactions()) {
            for (TransactionOutput to : t.getOutputs()) {
                rawItemsBuilder.add(to.getScriptBytes());
            }
        }
        ImmutableList<byte[]> rawItems = rawItemsBuilder.build();
        return build(rawItems, bip158p, k, bip158m);
    }
    
    public static GolombCodedSet build(Collection<byte[]> rawItems, int p, KeyParameter k, long m) {
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
        }
        int n = items.size();
        byte[] compressedSet = streamBytes.bytes();
        return new GolombCodedSet(n, compressedSet);
    }
    
    public int size() {
        return n;
    }
    
    public byte[] serialize() {
        return Bytes.concat(Ints.toByteArray(n), compressedSet);
    }
    
    private static SortedSet<Long> hashedSetConstruct(Collection<byte[]> rawItems, KeyParameter k, long m) {
        long n = (long) rawItems.size();
        long f = n * m;
        
        SortedSet<Long> out = new ConcurrentSkipListSet<>(UNSIGNED_LONG_COMPARATOR);
        for (byte[] rawItem : rawItems) {
            long val = hashToRange(rawItem, f, k);
            out.add(val);
        }
        
        return out;
    }

    public static void golombEncode(BitWriter writer, long x, int p) {
        for (long q = x >>> p; q > 0; q--) {
            writer.writeBit(1);
        }
        writer.writeBit(0);
        writer.write(x, p);
    }
    
    static long golombDecode(BitReader reader, int p) {
        int q = reader.readUntil(false);
        
        long r = reader.readLong(p);
        return ((long) q << p) + r;
    }

    private static long hashToRange(byte[] item, long f, KeyParameter k) {
        BigInteger hash = wrapLongUnsigned(sipHash(item, k));
        return hash
                .multiply(wrapLongUnsigned(f))
                .longValue();
    }
    
    private static long sipHash(byte[] item, KeyParameter k) {
        SipHash sipHash = new SipHash(2, 4);
        sipHash.init(k);
        sipHash.update(item, 0, item.length);
        return sipHash.doFinal();
    }
    
    private static BigInteger wrapLongUnsigned(long l) {
        return new BigInteger(1, Longs.toByteArray(l));
    }
}
