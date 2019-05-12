package org.bitcoinj.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.UnsignedLongs;
import org.bitcoinj.params.TestNet3Params;
import org.bouncycastle.crypto.params.KeyParameter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.bitcoinj.core.Utils.HEX;
import static org.bitcoinj.script.ScriptOpCodes.OP_RETURN;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class Bip158Test {
    private static final Logger log = LoggerFactory.getLogger(Bip158Test.class);
    
    private static final String TABLE_FILENAME = "bip158.json";
    
    private static final NetworkParameters TESTNET_PARAMS = TestNet3Params.get();
    
    private static final BitcoinSerializer BITCOIN_SERIALIZER = new BitcoinSerializer(TESTNET_PARAMS, false);
    
    private final TestCase testCase;

    public Bip158Test(TestCase testCase) {
        this.testCase = testCase;
    }
    
    @Parameterized.Parameters
    public static Iterable<TestCase> testData() throws Exception {
        InputStream tableStream = Bip158Test.class.getResourceAsStream(TABLE_FILENAME);
        InputStreamReader reader = new InputStreamReader(tableStream);
        JsonNode json = new ObjectMapper().readTree(reader);
        
        Iterator<JsonNode> jsonIterator = json.iterator();
        jsonIterator.next(); // skip the first line, it's metadata
        ImmutableList<JsonNode> testCaseJsonArrays = ImmutableList.copyOf(jsonIterator);
        
        List<DeserializeTestCase> tasks = new ArrayList<>();
        for (JsonNode testArray : testCaseJsonArrays) tasks.add(new DeserializeTestCase(testArray));
        
        ExecutorService executorService = Executors.newCachedThreadPool();
        List<Future<TestCase>> testCaseFutures = executorService.invokeAll(tasks);
        List<TestCase> testCases = new ArrayList<>();
        for (Future<TestCase> testCaseFuture : testCaseFutures) testCases.add(testCaseFuture.get());
        return testCases;
    }
    
    private static final class DeserializeTestCase implements Callable<TestCase> {
        
        private final JsonNode jsonNode;
        
        DeserializeTestCase(JsonNode jsonNode) {
            this.jsonNode = jsonNode;
        }

        @Override
        public TestCase call() {
            // has side effect of setting thread-local context
            new Context(TESTNET_PARAMS);
            Iterator<JsonNode> i = jsonNode.iterator();
            TestCase testCase = new TestCase();
            
            testCase.blockHeight = i.next().asLong();
            testCase.blockHash = Sha256Hash.wrap(i.next().asText());
            testCase.block = BITCOIN_SERIALIZER.makeBlock(HEX.decode(i.next().asText()));
            
            ImmutableList.Builder<byte[]> previousOutputScripts = new ImmutableList.Builder<>();
            for (JsonNode outputScriptNode : ImmutableList.copyOf(i.next().iterator())) {
                previousOutputScripts.add(HEX.decode(outputScriptNode.asText()));
            }
            testCase.previousOutputScripts = previousOutputScripts.build();
            
            testCase.previousBasicHeader = HEX.decode(i.next().asText());
            testCase.basicFilter = HEX.decode(i.next().asText());
            testCase.basicHeader = HEX.decode(i.next().asText());
            testCase.notes = i.next().asText();
            
            assertEquals(testCase.blockHash, testCase.block.getHash());
            
            return testCase;
        }
    }
    
    @Test
    public void testBuildAndSerialize() {
        GolombCodedSet gcs = GolombCodedSet.buildBip158(testCase.block, testCase.previousOutputScripts);
        assertArrayEquals(testCase.basicFilter, gcs.serialize());
    }

    @Test
    public void testDeserializeAndMatch() {
        GolombCodedSet gcs = GolombCodedSet.deserialize(testCase.basicFilter);
        byte[] blockHashLittleEndian = testCase.blockHash.getReversedBytes();
        KeyParameter k = new KeyParameter(blockHashLittleEndian, 0, 16);
        for (byte[] element : getFilterElements(testCase.block, testCase.previousOutputScripts)) {
            assertTrue(gcs.bip158match(k, element));
        }
    }

    private static ImmutableList<byte[]> getFilterElements(Block block, Iterable<byte[]> previousOutputScripts) {
        ImmutableList.Builder<byte[]> rawItemsBuilder = ImmutableList.builder();
        for (byte[] previousOutputScript : previousOutputScripts) {
            if (previousOutputScript.length > 0)
                rawItemsBuilder.add(previousOutputScript);
        }

        List<Transaction> transactions = block.getTransactions();
        if (transactions != null) {
            for (Transaction t : transactions) {
                for (TransactionOutput to : t.getOutputs()) {
                    byte[] script = to.getScriptBytes();
                    if (script.length > 1 && script[0] != (byte) OP_RETURN)
                        rawItemsBuilder.add(script);
                }
            }
        }

        return rawItemsBuilder.build();
    }

    private static final class TestCase {
        long blockHeight;
        Sha256Hash blockHash;
        Block block;
        ImmutableList<byte[]> previousOutputScripts;
        byte[] previousBasicHeader;
        byte[] basicFilter;
        byte[] basicHeader;
        String notes;
        
        @Override
        public String toString() {
            return "TestCase{" +
                    "blockHeight=" + blockHeight +
                    ", blockHash=" + blockHash +
                    ", block=" + block +
                    ", previousOutputScripts=" + previousOutputScripts +
                    ", previousBasicHeader='" + previousBasicHeader + '\'' +
                    ", basicFilter='" + basicFilter + '\'' +
                    ", basicHeader='" + basicHeader + '\'' +
                    ", notes='" + notes + '\'' +
                    '}';
        }
    }

}
