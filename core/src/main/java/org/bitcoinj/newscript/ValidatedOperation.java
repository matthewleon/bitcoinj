package org.bitcoinj.newscript;

import com.google.common.base.Preconditions;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;

public final class ValidatedOperation implements Operation {

    private static final byte[] EMPTY = new byte[]{};

    public final Opcode opcode;

    public final byte[] data;

    private ValidatedOperation(Opcode opcode, byte[] data) {
        this.opcode = opcode;
        this.data = data;
    }

    @Override
    public Opcode getOpcode() {
        return opcode;
    }

    @Override
    public byte[] getData() {
        return data;
    }

    // TODO
//    public static ValidatedOperation fromInput(InputStream input) {
//
//    }

    public static ValidatedOperation fromByte(byte b) {
        Opcode opcode = Opcode.fromByte(b);
        if (opcode == null) return null;
        return fromOpcode(opcode);
    }

    public static ValidatedOperation fromByteThrowing(byte b) throws RuntimeException {
        return fromOpcode(Opcode.fromByteThrowing(b));
    }

    public static ValidatedOperation fromOpcode(Opcode opcode) {
        return new ValidatedOperation(opcode, EMPTY);
    }

    public static ValidatedOperation fromOpcode(Opcode opcode, byte[] data) {
        Preconditions.checkNotNull(data);
        return new ValidatedOperation(opcode, data);
    }

    public byte[] getDataCopy() {
        return data.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidatedOperation other = (ValidatedOperation) o;
        return opcode == other.opcode && Arrays.equals(data, other.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(opcode, Arrays.hashCode(data));
    }

}
