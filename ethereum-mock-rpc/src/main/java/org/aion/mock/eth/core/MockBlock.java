package org.aion.mock.eth.core;

import lombok.Builder;
import lombok.Builder.Default;
import org.ethereum.core.*;
import org.ethereum.crypto.HashUtil;
import org.ethereum.trie.Trie;
import org.ethereum.trie.TrieImpl;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.RLP;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

public class MockBlock extends Block {

    private static final byte[] EMPTY_WORD32 = new byte[32];
    private static final byte[] EMPTY_ADDRESS = new byte[20];
    private static final byte[] EMPTY_BLOOM = new byte[256];

    @Default private byte[] mockParentHash = EMPTY_WORD32;
    @Default private byte[] mockUnclesHash = EMPTY_WORD32;
    @Default private byte[] mockCoinbase = EMPTY_ADDRESS;
    @Default private byte[] mockLogsBloom = EMPTY_BLOOM;
    @Default private byte[] mockDifficulty = ByteUtil.EMPTY_BYTE_ARRAY;
    private long mockNumber = 0L;
    @Default private byte[] mockGasLimit = new byte[0];
    @Default private long mockGasUsed = 0L;
    @Default long mockTimestamp;

    @Default private byte[] mockExtraData = EMPTY_WORD32;
    @Default private byte[] mockMixHash = EMPTY_WORD32;
    @Default private byte[] mockNonce = EMPTY_WORD32;
    @Default private byte[] mockStateRoot = EMPTY_WORD32;

    private final List<TransactionInfo> mockTransactionsList;
    @Default private final List<BlockHeader> mockUncleList;

    @lombok.Builder
    private MockBlock(@Nonnull final byte[] parentHash,
                     @Nonnull final byte[] unclesHash,
                     @Nonnull final byte[] coinbase,
                     @Nonnull final byte[] logsBloom,
                     @Nonnull final byte[] difficulty,
                     final long number,
                     @Nonnull final byte[] gasLimit,
                     final long gasUsed,
                     final long timestamp,
                     @Nonnull byte[] extraData,
                     @Nonnull byte[] mixHash,
                     @Nonnull byte[] nonce,
                     @Nonnull byte[] stateRoot,
                     @Nonnull List<TransactionInfo> transactionsList,
                     @Nonnull List<BlockHeader> uncleList) {
        super();
        mockParentHash = parentHash;
        mockUnclesHash = unclesHash;
        mockCoinbase = coinbase;
        mockLogsBloom = logsBloom;
        mockDifficulty = difficulty;
        mockNumber = number;
        mockGasLimit = gasLimit;
        mockGasUsed = gasUsed;
        mockTimestamp = timestamp;
        mockExtraData = extraData;
        mockMixHash = mixHash;
        mockNonce = nonce;
        mockStateRoot = stateRoot;
        mockTransactionsList = transactionsList;
        mockUncleList = uncleList;
        // calculate the correct roots

        getHeader().setParentHash(mockParentHash);
        getHeader().setUnclesHash(mockUnclesHash);
        getHeader().setCoinbase(mockCoinbase);
        getHeader().setDifficulty(mockDifficulty);
        getHeader().setNumber(mockNumber);
        getHeader().setGasLimit(mockGasLimit);
        getHeader().setGasUsed(mockGasUsed);
        getHeader().setLogsBloom(mockLogsBloom);
        getHeader().setTimestamp(mockTimestamp);
        getHeader().setExtraData(mockExtraData);
        getHeader().setMixHash(mockMixHash);
        getHeader().setNonce(mockNonce);
        getHeader().setStateRoot(mockStateRoot);

        final var transactions = transactionsList.stream()
                .map(info -> info.getReceipt().getTransaction())
                .collect(Collectors.toList());
        final var txTrieRoot = calcTxTrie(transactions);

        final var receipts = transactionsList.stream()
                .map(TransactionInfo::getReceipt)
                .collect(Collectors.toList());
        final var txReceiptRoot = calcReceiptsTrie(receipts);

        getHeader().setStateRoot(this.mockStateRoot);
        getHeader().setTransactionsRoot(txTrieRoot);
        getHeader().setReceiptsRoot(txReceiptRoot);
        this.transactionsList = transactions;
        this.uncleList = uncleList;
    }

    private static byte[] calcTxTrie(List<Transaction> transactions) {

        Trie txsState = new TrieImpl();

        if (transactions == null || transactions.isEmpty())
            return HashUtil.EMPTY_TRIE_HASH;

        for (int i = 0; i < transactions.size(); i++) {
            txsState.put(RLP.encodeInt(i), transactions.get(i).getEncoded());
        }
        return txsState.getRootHash();
    }

    private static byte[] calcReceiptsTrie(List<TransactionReceipt> receipts) {
        Trie receiptsTrie = new TrieImpl();

        if (receipts == null || receipts.isEmpty())
            return HashUtil.EMPTY_TRIE_HASH;

        for (int i = 0; i < receipts.size(); i++) {
            receiptsTrie.put(RLP.encodeInt(i), receipts.get(i).getReceiptTrieEncoded());
        }
        return receiptsTrie.getRootHash();
    }
}
