package org.bitcoinj.script;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Bytes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import static org.bitcoinj.script.ScriptOpCodes.*;
import static org.junit.Assert.assertArrayEquals;

@RunWith(value = Parameterized.class)
public class ScriptChunkToByteArrayTest {

    private static final Random RANDOM = new Random(42);

    @Parameterized.Parameter
    public TestCase testCase;

    @Parameterized.Parameters
    public static Collection<TestCase> parameters() {
        TestCase opcode = new TestCase(OP_IF, null, new byte[]{OP_IF});
        TestCase smallNum = new TestCase(OP_0, null, new byte[]{OP_0});

        List<TestCase> smallPush = new ArrayList<>(OP_PUSHDATA1);
        for (byte len = 1; len < OP_PUSHDATA1; len++) {
            smallPush.add(TestCase.withData(len, len, new byte[] {}));
        }

        TestCase pushData1 = TestCase.withData(OP_PUSHDATA1, 0xff, new byte[]{(byte) 0xff});
        TestCase pushData2 = TestCase.withData(OP_PUSHDATA2, 0x0102, new byte[]{0x02, 0x01});
        TestCase pushData4 = TestCase.withData(OP_PUSHDATA4, 0x0102, new byte[]{0x02, 0x01, 0, 0});

        return ImmutableList.<TestCase>builder()
                .add(opcode, smallNum)
                .addAll(smallPush)
                .add(pushData1, pushData2, pushData4)
                .build();
    }

    @Test
    public void testToByteArray() {
        assertArrayEquals(testCase.expectedBytes, testCase.scriptChunk.toByteArray());
    }

    private final static class TestCase {
        private final ScriptChunk scriptChunk;
        private final byte[] expectedBytes;

        private TestCase(int opcode, byte[] data, byte[] expectedBytes) {
            this.scriptChunk = new ScriptChunk(opcode, data);
            this.expectedBytes = expectedBytes;
        }

        private static TestCase withData(int opcode, int len, byte[] expectedEncodedLength) {
            byte[] bytes = new byte[len];
            RANDOM.nextBytes(bytes);
            byte[] expected = Bytes.concat(new byte[]{(byte) opcode}, expectedEncodedLength, bytes);
            return new TestCase(opcode, bytes, expected);
        }
    }
}