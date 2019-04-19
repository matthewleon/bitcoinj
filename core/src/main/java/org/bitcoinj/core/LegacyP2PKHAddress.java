package org.bitcoinj.core;

import org.bitcoinj.params.Networks;
import org.bitcoinj.script.Script;

import javax.annotation.Nullable;
import java.util.Arrays;

public class LegacyP2PKHAddress extends LegacyAddress {

    private LegacyP2PKHAddress(NetworkParameters params, byte[] hash160) throws AddressFormatException {
        super(params, hash160);
    }

    /**
     * Construct a {@link LegacyP2PKHAddress} that represents the given pubkey hash. The resulting address will be a P2PKH type of
     * address.
     *
     * @param params
     *            network this address is valid for
     * @param hash160
     *            20-byte pubkey hash
     * @return constructed address
     */
    public static LegacyP2PKHAddress fromPubKeyHash(NetworkParameters params, byte[] hash160) throws AddressFormatException {
        return new LegacyP2PKHAddress(params, hash160);
    }

    /**
     * Construct a {@link LegacyP2PKHAddress} that represents the public part of the given {@link ECKey}. Note that an address is
     * derived from a hash of the public key and is not the public key itself.
     *
     * @param params
     *            network this address is valid for
     * @param key
     *            only the public part is used
     * @return constructed address
     */
    public static LegacyP2PKHAddress fromKey(NetworkParameters params, ECKey key) {
        return fromPubKeyHash(params, key.getPubKeyHash());
    }

    public static LegacyP2PKHAddress fromBase58(@Nullable NetworkParameters params, String base58)
            throws AddressFormatException, AddressFormatException.WrongNetwork {
        byte[] versionAndDataBytes = Base58.decodeChecked(base58);
        int version = versionAndDataBytes[0] & 0xFF;
        byte[] bytes = Arrays.copyOfRange(versionAndDataBytes, 1, versionAndDataBytes.length);
        if (params == null) {
            for (NetworkParameters p : Networks.get()) {
                if (version == p.getAddressHeader())
                    return fromPubKeyHash(p, bytes);
            }
            throw new AddressFormatException.InvalidPrefix("No network found for " + base58);
        } else {
            if (version == params.getAddressHeader())
                return fromPubKeyHash(params, bytes);
            throw new AddressFormatException.WrongNetwork(version);
        }
    }

    @Override
    public int getVersion() {
        return params.getAddressHeader();
    }

    @Override
    public Script.ScriptType getOutputScriptType() {
        return Script.ScriptType.P2PKH;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        LegacyP2PKHAddress other = (LegacyP2PKHAddress) o;
        return super.equals(other);
    }

    @Override
    public LegacyP2PKHAddress clone() throws CloneNotSupportedException {
        return (LegacyP2PKHAddress) super.clone();
    }
}
