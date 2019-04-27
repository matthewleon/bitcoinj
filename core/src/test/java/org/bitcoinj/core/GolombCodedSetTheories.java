package org.bitcoinj.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.bitcoinj.params.TestNet3Params;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.bitcoinj.core.Utils.HEX;

@RunWith(Parameterized.class)
public class GolombCodedSetTheories {
    private static final Logger log = LoggerFactory.getLogger(GolombCodedSetTheories.class);
    
    private static final String TABLE_FILENAME = "bip158.json";
    
    private static final NetworkParameters TESTNET_PARAMS = TestNet3Params.get();
    
    private static final BitcoinSerializer BITCOIN_SERIALIZER = new BitcoinSerializer(TESTNET_PARAMS, false);
    
    private final TestCase testCase;

    public GolombCodedSetTheories(TestCase testCase) {
        this.testCase = testCase;
    }
    
    @Parameterized.Parameters
    public static Iterable<TestCase> testData() throws Exception {
        InputStream tableStream = GolombCodedSetTheories.class.getResourceAsStream(TABLE_FILENAME);
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
            testCase.blockPreviousOutputScripts = new ArrayList<>();
            for (JsonNode outputScriptNode : ImmutableList.copyOf(i.next().iterator())) {
                testCase.blockPreviousOutputScripts.add(outputScriptNode.asText());
            }
            testCase.previousBasicHeader = i.next().asText();
            testCase.basicFilter = i.next().asText();
            testCase.basicHeader = i.next().asText();
            testCase.notes = i.next().asText();
            
            return testCase;
        }
    }
    
    @Test
    public void show() {
        log.info(testCase.toString());
    }

    private static final class TestCase {
        long blockHeight;
        Sha256Hash blockHash;
        Block block;
        List<String> blockPreviousOutputScripts;
        String previousBasicHeader;
        String basicFilter;
        String basicHeader;
        String notes;

        @Override
        public String toString() {
            return "TestCase{" +
                    "blockHeight=" + blockHeight +
                    ", blockHash=" + blockHash +
                    ", block=" + block +
                    ", blockPreviousOutputScripts=" + blockPreviousOutputScripts +
                    ", previousBasicHeader='" + previousBasicHeader + '\'' +
                    ", basicFilter='" + basicFilter + '\'' +
                    ", basicHeader='" + basicHeader + '\'' +
                    ", notes='" + notes + '\'' +
                    '}';
        }
    }

}
