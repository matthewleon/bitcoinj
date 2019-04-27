package org.bitcoinj.core;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GolombCodedSetNonParameterizedTest {
    
    private static final Logger log = LoggerFactory.getLogger(GolombCodedSetNonParameterizedTest.class);
    
    @Test
    public void aTest() {
        log.info("{}", Long.MAX_VALUE - Long.MIN_VALUE);
        log.info("{}", Long.MIN_VALUE - Long.MAX_VALUE);
    }
}
