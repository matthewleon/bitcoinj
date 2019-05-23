package org.bitcoinj.newscript;

// TODO: generic association with InterpreterState?
public interface Interpreter<STATE> {

    STATE interpret(STATE state, Operation op);

    // or do we do something like boolean interpret(Script)
}
