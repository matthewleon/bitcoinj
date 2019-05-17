package org.bitcoinj.script;


import com.google.common.collect.ImmutableList;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;

@RunWith(value = Parameterized.class)
public class ScriptChunkSizeTest {

    private final int op;

    private final byte[] data;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> out = new ArrayList<>(256);

        for (int op = 0; op < 0xff; op++) {
        }
    }

}