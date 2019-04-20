package org.bitcoinj.core;

import org.bitcoinj.params.Networks;
import org.bitcoinj.script.Script;

import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * A legacy P2PKH (Pay 2 Public Key Hash) address is built by taking the RIPE-MD160 hash of the public key bytes,
 * with a version prefix and a checksum suffix, then encoding it textually as base58. The version prefix is used to
 * both denote the network for which the address is valid (see {@link NetworkParameters}, and also to indicate how the
 * bytes inside the address * should be interpreted (in this case, as P2PKH rather than P2SH).
 */
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

    /**
     * Construct a {@link LegacyP2PKHAddress} from its base58 form.
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
