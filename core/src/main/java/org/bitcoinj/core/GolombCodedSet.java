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

    private static final Comparator<Long> UNSIGNED_LONG_COMPARATOR = new Comparator<Long>() {
        @Override
        public int compare(Long o1, Long o2) {
            return UnsignedLongs.compare(o1, o2);
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
        
        byte[] blockHashLittleEndian = block.getHash().getReversedBytes();
        KeyParameter k = new KeyParameter(blockHashLittleEndian, 0, 16);
        
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
        
        ImmutableSet<Bytes> rawItems = rawItemsBuilder.build();
        return build(rawItems, bip158p, k, bip158m);
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
        int n = items.size();
        byte[] compressedSet = streamBytes.bytes();
        return new GolombCodedSet(n, compressedSet);
    }
    
    public int size() {
        return n;
    }
    
    public byte[] serialize() {
        byte[] size = new VarInt(n).encode();
        return concat(size, compressedSet);
    }
    
    private static SortedSet<Long> hashedSetConstruct(Set<Bytes> rawItems, KeyParameter k, long m) {
        long n = ((long) rawItems.size()) & 0xffffffffL;
        long f = n * m;
        
        SortedSet<Long> out = new ConcurrentSkipListSet<>(UNSIGNED_LONG_COMPARATOR);
        for (Bytes rawItem : rawItems) {
            out.add(hashToRange(rawItem, f, k));
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

    private static long hashToRange(Bytes item, long f, KeyParameter k) {
        final long lower32mask = 0xffffffffL;
        long hash = sipHashBigEndian(item.bytes, k);
        long a = hash >>> 32;
        long b = hash & lower32mask;
        long c = f >>> 32;
        long d = f & lower32mask;

        long ac = a * c;
        long ad = a * d;
        long bc = b * c;
        long bd = b * d;

        long mid34 = (bd >>> 32) + (bc & lower32mask) + (ad & lower32mask);
        return ac + (bc >>> 32) + (ad >>> 32) + (mid34 >>> 32);
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
