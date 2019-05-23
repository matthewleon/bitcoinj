package org.bitcoinj.newscript;

// might be necessary to make the operation type generic?
public interface StatefulInterpreter extends Interpreter<Void> {

    void interpret(Operation op);

}
