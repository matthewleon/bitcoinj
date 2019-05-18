package org.bitcoinj.script;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;

import static org.bitcoinj.script.ScriptOpCodes.*;

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

        // using ints to avoid trickiness with unsigned bytes
        ArrayList<ScriptChunk> pushData1 = new ArrayList<>(0xff);
        for (int b = 0; b < 0xff; b++) pushData1.add(new ScriptChunk(OP_PUSHDATA1, new byte[] {(byte) b}));

        ArrayList<ScriptChunk> pushData2 = new ArrayList<>(0xffff / 100);
        for (int i = 0; i < 0xffff; i+=100) pushData2.add(new ScriptChunk(OP_PUSHDATA2, Ints.toByteArray(i)));

        // using longs to avoid trickiness with unsigned ints
        ArrayList<ScriptChunk> pushData4 = new ArrayList<>(0xffffffff / 10_000);
        for (long l = 0; l < 0xffffL; l+=10_000) pushData2.add(new ScriptChunk(OP_PUSHDATA4, Longs.toByteArray(l)));

        return ImmutableList.<ScriptChunk>builder()
                .addAll(dataless)
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