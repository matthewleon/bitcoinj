package org.bitcoinj.core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;
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
import org.bitcoinj.script.ScriptChunk;
import org.bouncycastle.crypto.macs.SipHash;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.util.Pack;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

import static org.bitcoinj.script.ScriptOpCodes.OP_RETURN;

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
    
    public static GolombCodedSet buildBip158(Block block, Iterable<Script> previousOutputScripts) {
        final int bip158p = 19;
        final int bip158m = 784931;
        
        byte[] blockHashLittleEndian = block.getHash().getReversedBytes();
        KeyParameter k = new KeyParameter(blockHashLittleEndian, 0, 16);
        
        ImmutableList.Builder<byte[]> rawItemsBuilder = new ImmutableList.Builder<>();
        rawItemsBuilder.addAll(getHashableScriptBytes(previousOutputScripts));
        
        List<Transaction> transactions = block.getTransactions();
        List<Script> outputScripts = new ArrayList<>();
        if (transactions != null) {
            for (Transaction t : transactions) {
                for (TransactionOutput to : t.getOutputs()) {
                    outputScripts.add(to.getScriptPubKey());
                }
            }
        }
        rawItemsBuilder.addAll(getHashableScriptBytes(outputScripts));
        
        ImmutableList<byte[]> rawItems = rawItemsBuilder.build();
        return build(rawItems, bip158p, k, bip158m);
    }
    
    private static List<byte[]> getHashableScriptBytes(Iterable<Script> scripts) {
        ImmutableList.Builder<byte[]> rawItemsBuilder = new ImmutableList.Builder<>();
        
        for (Script script : scripts) {
            // exclude scripts beginning with OP_RETURN
            List<ScriptChunk> chunks = script.getChunks();
            if (chunks.size() > 0 && chunks.get(0).equalsOpCode(OP_RETURN))
                continue;

            // exclude empty scripts
            byte[] scriptBytes = script.getProgram();
            if (scriptBytes.length < 1)
                continue;

            rawItemsBuilder.add(scriptBytes);
        }
        return rawItemsBuilder.build();
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
        return Bytes.concat(size, compressedSet);
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
        BigInteger hash = wrapLongUnsigned(sipHashBigEndian(item, k));
        return hash
                .multiply(wrapLongUnsigned(f))
                .shiftRight(64)
                .longValue();
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
    
    private static BigInteger wrapLongUnsigned(long l) {
        return new BigInteger(1, Longs.toByteArray(l));
    }
}
