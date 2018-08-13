package org.aion.mock.eth.state;


import org.ethereum.core.Block;
import org.ethereum.core.TransactionInfo;
import org.ethereum.db.ByteArrayWrapper;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ChainState {

    private Map<ByteArrayWrapper, Block> blockHashMap = new HashMap<>();

    private Map<Long, List<Block>> blockNumberMap = new HashMap<>();

    private Map<ByteArrayWrapper, TransactionInfo> transactionInfoMap = new HashMap<>();

    // swaps between different block indices, used to switch between forks
    private int chainIndex = 0;

    // best public block number, visible to the user
    private long headBlockNumber = 0L;

    // best block number of the chain
    private long chainBlockNumber = 0L;


    // TODO: mock, there is a better locking strategy, implement later

    /**
     * Adds a new block into the chain state, some simple verification checks
     * to ensure (within the context of the mock that things are consistent
     */
    public synchronized void addBlock(@Nonnull final Block block, List<TransactionInfo> infos, long index) {
        checkBlock(block);

        if (this.blockHashMap.isEmpty()) {
            chainBlockNumber = block.getNumber();
        }
        this.blockHashMap.put(new ByteArrayWrapper(block.getHash()), block);

        // places block into

        // TODO: should assert that infos match block transactions
        for (var info : infos) {
            this.transactionInfoMap.put(wrap(info.getReceipt().getTransaction().getHash()), info);
        }
    }

    public synchronized Block getBlock(@Nonnull final byte[] blockHash) {
        return this.blockHashMap.get(new ByteArrayWrapper(blockHash));
    }

    public synchronized Block getBlock(long blockNumber) {
        if (chainIndex > this.blockNumberMap.get(blockNumber).size() - 1)
            throw new RuntimeException("tried to executeTransferPayload getBlock on non-existent chainIndex: " + chainIndex);
        return this.blockNumberMap.get(blockNumber).get(chainIndex);
    }

    public synchronized int getChainIndex() {
        return chainIndex;
    }

    public synchronized void setChainIndex(int chainIndex) {
        this.chainIndex = chainIndex;
    }

    public synchronized long getHeadBlockNumber() {
        return headBlockNumber;
    }

    public synchronized void setHeadBlockNumber(long headBlockNumber) {
        this.headBlockNumber = headBlockNumber;
    }

    public synchronized long getChainBlockNumber() {
        return chainBlockNumber;
    }

    public synchronized void setChainBlockNumber(long chainBlockNumber) {
        this.chainBlockNumber = chainBlockNumber;
    }

    public synchronized TransactionInfo getTransactionInfo(byte[] transactionHash) {
        return this.transactionInfoMap.get(wrap(transactionHash));
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
            throw new RuntimeException("cannot throw block number");
    }

    private static ByteArrayWrapper wrap(@Nonnull final byte[] input) {
        return new ByteArrayWrapper(input);
    }
}
