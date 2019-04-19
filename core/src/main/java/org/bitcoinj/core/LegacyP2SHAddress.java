package org.bitcoinj.core;

import org.bitcoinj.params.Networks;
import org.bitcoinj.script.Script;

import javax.annotation.Nullable;
import java.util.Arrays;

public class LegacyP2SHAddress extends LegacyAddress {

    private LegacyP2SHAddress(NetworkParameters params, byte[] hash160) throws AddressFormatException {
        super(params, hash160);
    }

    /**
     * Construct a {@link LegacyP2SHAddress} that represents the given P2SH script hash.
     *
     * @param params
     *            network this address is valid for
     * @param hash160
     *            P2SH script hash
     * @return constructed address
     */
    public static LegacyP2SHAddress fromScriptHash(NetworkParameters params, byte[] hash160) {
        return new LegacyP2SHAddress(params, hash160);
    }

    public static LegacyP2SHAddress fromBase58(@Nullable NetworkParameters params, String base58)
            throws AddressFormatException, AddressFormatException.WrongNetwork {
        byte[] versionAndDataBytes = Base58.decodeChecked(base58);
        int version = versionAndDataBytes[0] & 0xFF;
        byte[] bytes = Arrays.copyOfRange(versionAndDataBytes, 1, versionAndDataBytes.length);
        if (params == null) {
            for (NetworkParameters p : Networks.get()) {
                if (version == p.getAddressHeader())
                    return fromScriptHash(p, bytes);
            }
            throw new AddressFormatException.InvalidPrefix("No network found for " + base58);
        } else {
            if (version == params.getAddressHeader())
                return fromScriptHash(params, bytes);
            throw new AddressFormatException.WrongNetwork(version);
        }
    }

    @Override
    public int getVersion() {
        return params.getP2SHHeader();
    }

    @Override
    public Script.ScriptType getOutputScriptType() {
        return Script.ScriptType.P2SH;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        LegacyP2SHAddress other = (LegacyP2SHAddress) o;
        return super.equals(other);
    }

    @Override
    public LegacyP2SHAddress clone() throws CloneNotSupportedException {
        return (LegacyP2SHAddress) super.clone();
    }
}
