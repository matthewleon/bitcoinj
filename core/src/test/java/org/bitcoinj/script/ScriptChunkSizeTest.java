package org.bitcoinj.script;

import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;

import static org.bitcoinj.script.ScriptOpCodes.*;

/**
 * ScriptChunk.size() determines the size of a serialized ScriptChunk without actually performing serialization.
 * This parameterized test is meant to exhaustively prove that the method does what it promises.
 */
@RunWith(value = Parameterized.class)
public class ScriptChunkSizeTest {

    private final ScriptChunk scriptChunk;

    public ScriptChunkSizeTest(ScriptChunk scriptChunk) {
        this.scriptChunk = scriptChunk;
    }

    @Parameterized.Parameters
    public static Collection<ScriptChunk> data() {
        ArrayList<ScriptChunk> dataless = new ArrayList<>(0xff);
        for (int op = 0; op < 0xff; op++) dataless.add(new ScriptChunk(op, null));

        ArrayList<ScriptChunk> smallData = new ArrayList<>(0x4b - 1);
        for (int op = 1; op < 0x4b; op++) {
            byte[] data = new byte[op];
            smallData.add(new ScriptChunk(op, data));
        }

        // using ints to avoid trickiness with unsigned bytes
        ArrayList<ScriptChunk> pushData1 = new ArrayList<>(0xff);
        for (int i = 0; i < 0xff; i++) pushData1.add(new ScriptChunk(OP_PUSHDATA1, new byte[i]));

        ArrayList<ScriptChunk> pushData2 = new ArrayList<>(0xffff / 100);
        for (int i = 0; i < Script.MAX_SCRIPT_ELEMENT_SIZE; i++)
            pushData2.add(new ScriptChunk(OP_PUSHDATA2, new byte[i]));

        ArrayList<ScriptChunk> pushData4 = new ArrayList<>(0xffffffff / 10_000);
        for (int i = 0; i < Script.MAX_SCRIPT_ELEMENT_SIZE; i+=10_000)
            pushData4.add(new ScriptChunk(OP_PUSHDATA4, new byte[i]));

        return ImmutableList.<ScriptChunk>builder()
                .addAll(dataless)
                .addAll(smallData)
                .addAll(pushData1)
                .addAll(pushData2)
                .addAll(pushData4)
                .build();
    }

    @Test
    public void testSize() {
        Assert.assertEquals(scriptChunk.toByteArray().length, scriptChunk.size());
    }
}