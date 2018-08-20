package org.aion.mock.eth;

import org.aion.mock.eth.populate.PopulationStrategy;
import org.aion.mock.eth.state.ChainState;
import org.ethereum.core.Block;
import org.ethereum.core.Transaction;
import org.ethereum.core.TransactionInfo;
import org.ethereum.core.TransactionReceipt;

import java.util.Properties;

public class DefaultChainFacade implements ChainFacade {

    private final PopulationStrategy strategy;
    private final ChainState chainState;

    public DefaultChainFacade(PopulationStrategy strategy, ChainState chainState) {
        this.strategy = strategy;
        this.strategy.populateInitial();
        this.chainState = chainState;
    }

    @Override
    public Block getBlockByNumber(long number) {
        Properties props = new Properties();
        props.put("number", number);
        this.strategy.populateStep(props);
        return this.chainState.getBlock(number);
    }

    @Override
    public Block getBlockByHash(byte[] blockHash) {
        return this.chainState.getBlock(blockHash);
    }

    @Override
    public Transaction getTransactionByHash(byte[] transactionHash) {
        var info = this.chainState.getTransactionInfo(transactionHash);
        if (info == null) return null;
        return info.getReceipt().getTransaction();
    }

    @Override
    public TransactionReceipt getTransactionReceiptByHash(byte[] transactionHash) {
        this.strategy.populateStep(new Properties());
        var info = this.chainState.getTransactionInfo(transactionHash);
        if (info == null) return null;
        return info.getReceipt();
    }

    @Override
    public TransactionInfo getTransactionInfo(byte[] transactionHash) {
        this.strategy.populateStep(new Properties());
        return this.chainState.getTransactionInfo(transactionHash);
    }

    @Override
    public long getBlockNumber() {
        this.strategy.populateStep(new Properties());
        return this.chainState.getHeadBlockNumber();
    }

    @Override
    public Block getBestBlock() {
        this.strategy.populateStep(new Properties());
        return this.chainState.getBlock(this.chainState.getHeadBlockNumber());
    }
}
