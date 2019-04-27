package org.bitcoinj.core;

import com.google.common.collect.ImmutableList;
import com.tomgibara.bits.BitReader;
import com.tomgibara.bits.BitWriter;
import com.tomgibara.bits.Bits;
import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.StreamBytes;
import com.tomgibara.streams.Streams;
import com.tomgibara.streams.WriteStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class GolombCodedSetTest {

    private static final Logger log = LoggerFactory.getLogger(GolombCodedSetTest.class);
    
    private static final long[] inputs = new long[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    
    @Parameterized.Parameters
    public static Collection<Integer> data() {
        return ImmutableList.of(2, 4, 5, 6, 9, 12);
    }
    
    private int p;
    
    public GolombCodedSetTest(int p) {
        this.p = p;
    }

    @Test
    public void golombRoundtrip() {
        StreamBytes streamBytes = Streams.bytes();
        try (WriteStream writeStream = streamBytes.writeStream()) {
            BitWriter writer = Bits.writerTo(writeStream);
            for (long input : inputs) {
                GolombCodedSet.golombEncode(writer, input, p);
            }
            writer.flush();
        }
        BitReader reader = Bits.readerFrom(streamBytes.bytes());
        for (long x : inputs) {
            assertEquals(x, GolombCodedSet.golombDecode(reader, p));
        }
    }

}