package org.bitcoinj.newscript;

// maybe use some kind of forwarding technique for delegation instead?
public final class StatefulWrappedInterpreter<STATE> extends BaseStatefulInterpreter {

    private final Interpreter<STATE> interpreter;

    private STATE state;

    private StatefulWrappedInterpreter(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    public final void interpret(Operation operation) {
        state = interpreter.interpret(state, operation);
    }

}
