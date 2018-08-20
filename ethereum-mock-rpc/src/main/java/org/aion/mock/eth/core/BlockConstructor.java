package org.aion.mock.eth.core;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Singular;
import org.ethereum.core.*;
import org.ethereum.crypto.HashUtil;
import org.ethereum.trie.Trie;
import org.ethereum.trie.TrieImpl;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.RLP;

import java.util.List;
import java.util.stream.Collectors;

@Builder
public class BlockConstructor {

    private static final byte[] EMPTY_WORD32 = new byte[32];
    private static final byte[] EMPTY_ADDRESS = new byte[20];
    private static final byte[] EMPTY_BLOOM = new byte[256];

    @Default
    @Builder.ObtainVia(method="parentHash")
    private byte[] parentHash = EMPTY_WORD32;

    @Default
    @Builder.ObtainVia(method="unclesHash")
    private byte[] unclesHash = EMPTY_WORD32;

    @Default
    @Builder.ObtainVia(method="coinbase")
    private byte[] coinbase = EMPTY_ADDRESS;

    @Default
    @Builder.ObtainVia(method="logsBloom")
    private byte[] logsBloom = EMPTY_BLOOM;

    @Default
    @Builder.ObtainVia(method="difficulty")
    private byte[] difficulty = ByteUtil.EMPTY_BYTE_ARRAY;

    @Builder.ObtainVia(method="number")
    private long number = 0L;

    @Default
    @Builder.ObtainVia(method="gasLimit")
    private byte[] gasLimit = new byte[0];

    @Default
    @Builder.ObtainVia(method="gasUsed")
    private long gasUsed = 0L;

    @Default
    @Builder.ObtainVia(method="timestamp")
    long timestamp;

    @Default
    @Builder.ObtainVia(method="extraData")
    private byte[] extraData = EMPTY_WORD32;

    @Default
    @Builder.ObtainVia(method="mixHash")
    private byte[] mixHash = EMPTY_WORD32;

    @Default
    @Builder.ObtainVia(method="nonce")
    private byte[] nonce = EMPTY_WORD32;

    @Default
    @Builder.ObtainVia(method="stateRoot")
    private byte[] stateRoot = EMPTY_WORD32;

    @Singular("transactionsList")
    @Builder.ObtainVia(method="transactionsList")
    private List<TransactionInfo> transactionsList;

    @Singular("uncleList")
    @Builder.ObtainVia(method="uncleList")
    private List<BlockHeader> uncleList;

    private Block block;

    private BlockHeader getHeader() {
        return this.block.getHeader();
    }

    public Block buildBlock() {

        final var transactions = transactionsList.stream()
                .map(info -> info.getReceipt().getTransaction())
                .collect(Collectors.toList());
        final var txTrieRoot = calcTxTrie(transactions);

        final var receipts = transactionsList.stream()
                .map(TransactionInfo::getReceipt)
                .collect(Collectors.toList());
        final var txReceiptRoot = calcReceiptsTrie(receipts);

        final var receiptBloom = new Bloom();
        for (var r : receipts) {
            receiptBloom.or(r.getBloomFilter());
        }

        if (this.number == 1) {
            System.out.println("sure");
        }

        this.block = new Block(
                parentHash,
                unclesHash,
                coinbase,
                receiptBloom.getData(),
                difficulty,
                number,
                gasLimit,
                gasUsed,
                timestamp,
                extraData,
                mixHash,
                nonce,
                stateRoot,
                txTrieRoot,
                txReceiptRoot,
                transactions,
                uncleList
        );

        return this.block;
    }

    public static byte[] calcTxTrie(List<Transaction> transactions) {

        Trie txsState = new TrieImpl();

        if (transactions == null || transactions.isEmpty())
            return HashUtil.EMPTY_TRIE_HASH;

        for (int i = 0; i < transactions.size(); i++) {
            txsState.put(RLP.encodeInt(i), transactions.get(i).getEncoded());
        }
        return txsState.getRootHash();
    }

    public static byte[] calcReceiptsTrie(List<TransactionReceipt> receipts) {
        Trie receiptsTrie = new TrieImpl();

        if (receipts == null || receipts.isEmpty())
            return HashUtil.EMPTY_TRIE_HASH;

        for (int i = 0; i < receipts.size(); i++) {
            receiptsTrie.put(RLP.encodeInt(i), receipts.get(i).getReceiptTrieEncoded());
        }
        return receiptsTrie.getRootHash();
    }
}
