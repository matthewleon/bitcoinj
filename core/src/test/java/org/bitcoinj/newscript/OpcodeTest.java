package org.bitcoinj.newscript;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

import java.util.EnumSet;

import static org.junit.Assert.*;

public class OpcodeTest {
    private static final EnumSet<Opcode> ALL_OPCODES = EnumSet.allOf(Opcode.class);

    private static final EnumSet<Opcode> REDUNDANT_OPCODES = EnumSet.of(
            Opcode.OP_FALSE,
            Opcode.OP_TRUE,
            Opcode.OP_NOP2,
            Opcode.OP_NOP3
    );

    @Test
    public void equalsAndHashcode() {
        EqualsVerifier.forClass(Opcode.class)
                .verify();
    }

    @Test
    public void fromByteSuccess() {
        Sets.SetView<Opcode> nonRepeatedOpcodes = Sets.difference(ALL_OPCODES, REDUNDANT_OPCODES);

        for (Opcode opcode : nonRepeatedOpcodes) {
            // check that opcode is not in redundant opcodes?
            assertEquals(opcode, Opcode.fromByte(opcode.op));
        }
    }

    @Test
    public void fromByteSuccessRepeatedOpcodes() {
        for (Opcode opcode : REDUNDANT_OPCODES) {
            Opcode returnedOpcode = Opcode.fromByte(opcode.op);
            assertNotNull(returnedOpcode);
            assertNotEquals(opcode, returnedOpcode);
        }
    }

    @Test
    public void fromByteNull() {
        ImmutableSet.Builder<Byte> builder = ImmutableSet.builder();
        for (Opcode opcode : ALL_OPCODES) builder.add(opcode.op);
        ImmutableSet<Byte> opcodeBytes = builder.build();
        for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
            byte b = (byte) i;
            if (!opcodeBytes.contains(b)) assertNull(Opcode.fromByte(b));
        }
    }

}