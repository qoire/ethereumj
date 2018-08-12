package org.aion.mock.eth.state;


import org.ethereum.core.Block;
import org.ethereum.core.Transaction;
import org.ethereum.core.TransactionReceipt;
import org.ethereum.db.ByteArrayWrapper;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChainState {

    private Map<ByteArrayWrapper, Block> blockHashMap = new HashMap<>();

    private Map<Long, List<Block>> blockNumberMap = new HashMap<>();

    private Map<ByteArrayWrapper, Transaction> transactionMap = new HashMap<>();

    private Map<ByteArrayWrapper, TransactionReceipt> receiptMap = new HashMap<>();

    private int chainIndex = 0;

    /**
     * Adds a new block into the chain state, some simple verification checks
     * to ensure (within the context of the mock that things are consistent
     * @param block
     */
    public void addBlock(@Nonnull final Block block, long index) {
        checkBlock(block);
        this.blockHashMap.put(new ByteArrayWrapper(block.getHash()), block);

        for (Transaction tx : block.getTransactionsList()) {
            this.transactionMap.put(new ByteArrayWrapper(tx.getRawHash()), tx);
        }
    }

    public Block getBlock(@Nonnull final byte[] blockHash) {
        return this.blockHashMap.get(new ByteArrayWrapper(blockHash));
    }

    public Block getBlock(long blockNumber) {
        if (chainIndex > this.blockNumberMap.get(blockNumber).size() - 1)
            throw new RuntimeException("tried to execute getBlock on non-existent chainIndex: " + chainIndex);

        return this.blockNumberMap.get(blockNumber).get(chainIndex);
    }

    protected void checkBlock(@Nonnull final Block block) {
        assert block.getParentHash() != null;

        if (!blockHashMap.isEmpty())
            assert blockHashMap.containsKey(wrap(block.getParentHash()));
        assert !blockHashMap.containsKey(wrap(block.getHash()));

        assert block.getCoinbase() != null;
        assert block.getDifficulty() != null;
        assert block.getDifficultyBI() != null;
        assert block.getTxTrieRoot() != null;
        assert block.getReceiptsRoot() != null;
        assert block.getLogBloom() != null;
        assert block.getTransactionsList() != null;
        assert block.getTimestamp() > blockHashMap.get(wrap(block.getParentHash())).getTimestamp();
    }

    protected void insertBlockNumber(@Nonnull final Block block, long index) {
        if (this.blockNumberMap.get(index) == null && index != 0)
            throw new RuntimeException("")
    }

    private static ByteArrayWrapper wrap(@Nonnull final byte[] input) {
        return new ByteArrayWrapper(input);
    }
}
