package org.bitcoinj.newscript;

import java.io.InputStream;
import java.util.Iterator;

public interface Parser {

    Iterator<Operation> parse(InputStream inBytes);

}
