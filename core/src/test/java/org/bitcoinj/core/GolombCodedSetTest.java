package org.bitcoinj.core;

import com.tomgibara.bits.BitReader;
import com.tomgibara.bits.BitWriter;
import com.tomgibara.bits.Bits;
import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.StreamBytes;
import com.tomgibara.streams.Streams;
import com.tomgibara.streams.WriteStream;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

import static org.junit.Assert.*;

public class GolombCodedSetTest {

    private static final Logger log = LoggerFactory.getLogger(GolombCodedSetTest.class);

    @Test
    public void golombRoundtrip() {
        StreamBytes bytes = Streams.bytes();
        try (WriteStream writeStream = bytes.writeStream()) {
            BitWriter writer = Bits.writerTo(writeStream);
            GolombCodedSet.golombEncode(writer, 5, 2);
            GolombCodedSet.golombEncode(writer, 9, 2);
            writer.flush();
        }
        byte[] out = bytes.bytes();
        int i = new BigInteger(1, out).intValueExact();
        log.info("bin: {}", Integer.toBinaryString(i));
        
        BitReader reader = Bits.readerFrom(out);
        log.info("long 1: {}", GolombCodedSet.golombDecode(reader, 2));
        log.info("long 2: {}", GolombCodedSet.golombDecode(reader, 2));
    }

}