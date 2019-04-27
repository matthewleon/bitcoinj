package org.bitcoinj.core;

import com.google.common.primitives.Longs;
import com.tomgibara.bits.BitReader;
import com.tomgibara.bits.BitWriter;
import org.bouncycastle.crypto.macs.SipHash;
import org.bouncycastle.crypto.params.KeyParameter;
import sun.jvm.hotspot.runtime.Bytes;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class GolombCodedSet {
    
    private static final SipHash SIP_HASH = new SipHash(2, 4);
    
    private byte[] compressedSet;
    
    static List<Long> hashedSetConstruct(byte[][] raw_items, KeyParameter k, long m) {
        long n = (long) raw_items.length;
        long f = n * m;
        
        List<Long> out = new ArrayList<>(raw_items.length);
        for (byte[] raw_item : raw_items) {
            long val = hashToRange(raw_item, f, k);
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
    
    private synchronized static long sipHash(byte[] item, KeyParameter k) {
        // siphash operations are stateful, so we synchronize for thread-safety
        SIP_HASH.init(k);
        SIP_HASH.update(item, 0, item.length);
        return SIP_HASH.doFinal();
    }
    
    private static BigInteger wrapLongUnsigned(long l) {
        return new BigInteger(1, Longs.toByteArray(l));
    }
}
