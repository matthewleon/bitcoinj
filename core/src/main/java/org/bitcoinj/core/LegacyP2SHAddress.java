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

    /**
     * Construct a {@link LegacyP2SHAddress} from its base58 form.
     *
     * @param params
     *            expected network this address is valid for, or null if if the network should be derived from the
     *            base58
     * @param base58
     *            base58-encoded textual form of the address
     * @throws AddressFormatException
     *             if the given base58 doesn't parse or the checksum is invalid
     * @throws AddressFormatException.WrongNetwork
     *             if the given address is valid but for a different chain (eg testnet vs mainnet)
     */
    public static LegacyP2SHAddress fromBase58(@Nullable NetworkParameters params, String base58)
            throws AddressFormatException, AddressFormatException.WrongNetwork {
        byte[] versionAndDataBytes = Base58.decodeChecked(base58);
        int version = versionAndDataBytes[0] & 0xFF;
        byte[] bytes = Arrays.copyOfRange(versionAndDataBytes, 1, versionAndDataBytes.length);
        if (params == null) {
            for (NetworkParameters p : Networks.get()) {
                if (version == p.getP2SHHeader())
                    return fromScriptHash(p, bytes);
            }
            throw new AddressFormatException.InvalidPrefix("No network found for " + base58);
        } else {
            if (version == params.getP2SHHeader())
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
