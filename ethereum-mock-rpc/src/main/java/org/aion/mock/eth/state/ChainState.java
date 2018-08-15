package org.aion.mock.eth.state;


import lombok.extern.slf4j.Slf4j;
import org.ethereum.core.Block;
import org.ethereum.core.TransactionInfo;
import org.ethereum.db.ByteArrayWrapper;

import javax.annotation.Nonnull;
import java.util.*;


@Slf4j
public class ChainState {

    private Map<ByteArrayWrapper, Block> blockHashMap = new HashMap<>();

    private Map<Long, Map<String, Block>> blockNumberMap = new HashMap<>();

    private Map<ByteArrayWrapper, TransactionInfo> transactionInfoMap = new HashMap<>();

    // swaps between different block indices, used to switch between forks
    private String currentFork = "";

    // best public block number, visible to the user
    private long headBlockNumber = 0L;

    // best block number of the chain
    private long chainBlockNumber = 0L;

    // TODO: mock, there is a better locking strategy, implement later

    /**
     * Adds a new block into the chain state, some simple verification checks
     * to ensure (within the context of the mock that things are consistent
     */
    public synchronized void addBlock(@Nonnull final Block block, List<TransactionInfo> infos, String fork) {
        checkBlock(block);

        if (this.blockHashMap.isEmpty()) {
            chainBlockNumber = block.getNumber();
        }
        this.blockHashMap.put(new ByteArrayWrapper(block.getHash()), block);

        if (this.blockNumberMap.get(block.getNumber()) == null) {
            Map<String, Block> levelForkBlockMap = new HashMap<>();
            levelForkBlockMap.put(fork, block);
            this.blockNumberMap.put(block.getNumber(), levelForkBlockMap);
        } else {
            var levelForkBlockMap = this.blockNumberMap.get(block.getNumber());
            if (levelForkBlockMap.containsKey(fork))
                throw new RuntimeException("attempted to add two blocks, same fork same number");
            // otherwise, we know they're unique
            levelForkBlockMap.put(fork, block);
        }

        // TODO: should assert that infos match block transactions
        for (var info : infos) {
            this.transactionInfoMap.put(wrap(info.getReceipt().getTransaction().getHash()), info);
        }
    }

    public synchronized void setCurrentFork(@Nonnull final String fork) {
        this.currentFork = fork;
    }

    public synchronized String getCurrentFork() {
        return this.currentFork;
    }

    public synchronized Block getBlock(@Nonnull final byte[] blockHash) {
        return this.blockHashMap.get(new ByteArrayWrapper(blockHash));
    }

    public synchronized Block getBlock(long blockNumber) {
        if (blockNumber > this.headBlockNumber)
            return null;
        return this.blockNumberMap.get(blockNumber).get(this.currentFork);
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
        assert block.getCoinbase() != null;
        assert block.getDifficulty() != null;
        assert block.getDifficultyBI() != null;
        assert block.getTxTrieRoot() != null;
        assert block.getReceiptsRoot() != null;
        assert block.getLogBloom() != null;
        assert block.getTransactionsList() != null;
        assert block.getTimestamp() > blockHashMap.get(wrap(block.getParentHash())).getTimestamp();
    }

    private static ByteArrayWrapper wrap(@Nonnull final byte[] input) {
        return new ByteArrayWrapper(input);
    }
}
