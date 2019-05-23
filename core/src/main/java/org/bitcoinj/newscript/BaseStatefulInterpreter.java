package org.bitcoinj.newscript;

public abstract class BaseStatefulInterpreter implements StatefulInterpreter {

    @Override
    public final Void interpret(Void state, Operation validatedOperation) {
        interpret(validatedOperation);
        return null;
    }

}
