package org.bitcoinj.script;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Random;

import static org.bitcoinj.script.ScriptOpCodes.OP_IF;
import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
public class ScriptChunkParseTest {

    private static Random RANDOM = new Random(42);

    @Parameterized.Parameter
    public TestCase testCase;

    @Parameterized.Parameters
    public static Collection<TestCase> data() {
        TestCase opcodeSuccess = TestCase.success(OP_IF, null, new byte[] {OP_IF});

        TestCase emptyFailure = TestCase.failure(new Exception(), new byte[] {});

        return ImmutableList.<TestCase>builder()
                .add(opcodeSuccess, emptyFailure)
                .build();
    }

    private static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    @Test
    public void testParseFromStream() {
        testCase.check();
    }

    private static class TestCase {
        private final ScriptChunk expected;
        private final Exception exception;
        private final ByteArrayInputStream input;

        private TestCase(@Nullable ScriptChunk expected, @Nullable Exception exception, byte[] input) {
            if (expected == null && exception == null)
                throw new RuntimeException("Test case requires checking either return value or exception.");
            if (expected != null && exception != null)
                throw new RuntimeException("Test case requires checking return value or exception, not both.");

            this.expected = expected;
            this.exception = exception;
            this.input = new ByteArrayInputStream(input);
        }

        static TestCase success(int opcode, @Nullable byte[] data, byte[] input) {
            return new TestCase(new ScriptChunk(opcode, data), null, input);
        }

        static TestCase failure(Exception exception, byte[] input) {
            return new TestCase(null, exception, input);
        }

        private void check() {
            ScriptChunk result;
            try {
                result = ScriptChunk.parseFromStream(input);
            } catch (Exception e) {
                if (exception != null) {
                    assertEquals(exception, e);
                    return;
                }
                throw e;
            }
            assertEquals(expected, result);
        }
    }
}
