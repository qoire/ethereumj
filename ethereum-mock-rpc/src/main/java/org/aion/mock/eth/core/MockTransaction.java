package org.aion.mock.eth.core;

import org.ethereum.core.Transaction;
import org.ethereum.crypto.HashUtil;
import org.ethereum.util.RLP;

import javax.annotation.Nonnull;

import static org.ethereum.util.ByteUtil.EMPTY_BYTE_ARRAY;

public class MockTransaction extends Transaction {

    public MockTransaction(@Nonnull final byte[] nonce,
                           @Nonnull final byte[] gasPrice,
                           @Nonnull final byte[] gasLimit,
                           @Nonnull final byte[] sendAddress,
                           @Nonnull final byte[] receiveAddress,
                           @Nonnull final byte[] value,
                           @Nonnull final byte[] data,
                           @Nonnull final Integer chainId) {
        super(nonce, gasPrice, gasLimit, receiveAddress, value, data, chainId);
        this.sendAddress = sendAddress;
    }

    @Override
    public byte[] getSender() {
        return this.sendAddress;
    }

    @Override
    public byte[] getEncoded() {

        if (rlpEncoded != null) return rlpEncoded;

        // parse null as 0 for nonce
        byte[] nonce = null;
        if (this.nonce == null || this.nonce.length == 1 && this.nonce[0] == 0) {
            nonce = RLP.encodeElement(null);
        } else {
            nonce = RLP.encodeElement(this.nonce);
        }
        byte[] gasPrice = RLP.encodeElement(this.gasPrice);
        byte[] gasLimit = RLP.encodeElement(this.gasLimit);
        byte[] receiveAddress = RLP.encodeElement(this.receiveAddress);
        byte[] value = RLP.encodeElement(this.value);
        byte[] data = RLP.encodeElement(this.data);
        byte[] senderAddress = RLP.encodeElement(this.sendAddress);

        this.rlpEncoded = RLP.encodeList(nonce, gasPrice, gasLimit,
                receiveAddress, value, data, senderAddress);

        this.hash = HashUtil.sha3(rlpEncoded);

        return rlpEncoded;
    }
}
