package org.bitcoinj.newscript;

import com.google.common.collect.ForwardingCollection;
import com.google.common.collect.ImmutableList;

import java.util.List;

// okay, not sure I know what to do with this
public final class Script extends ForwardingCollection<ValidatedOperation> {

    private final ImmutableList<ValidatedOperation> validatedOperations;

    private Script(ImmutableList<ValidatedOperation> validatedOperations) {
        this.validatedOperations = validatedOperations;
    }

    public static Script fromIterable(Iterable<ValidatedOperation> it) {
        return new Script(ImmutableList.copyOf(it));
    }

    @Override
    protected List<ValidatedOperation> delegate() {
        return validatedOperations;
    }
}
