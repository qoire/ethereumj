package org.aion.mock.eth;

import org.aion.mock.eth.populate.PopulationStrategy;
import org.aion.mock.eth.state.ChainState;
import org.ethereum.core.Block;
import org.ethereum.core.Transaction;
import org.ethereum.core.TransactionInfo;
import org.ethereum.core.TransactionReceipt;
import org.ethereum.util.ByteUtil;

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
        var props = new Properties();
        props.setProperty("getBlockByNumber", "");
        props.setProperty("number", Long.toString(number));
        this.strategy.populateStep(props);
        return this.chainState.getBlock(number);
    }

    @Override
    public Block getBlockByHash(byte[] blockHash) {
        var props = new Properties();
        props.setProperty("getBlockByHash", "");
        props.setProperty("hash", ByteUtil.toHexString(blockHash));
        this.strategy.populateStep(props);
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
        var props = new Properties();
        props.setProperty("getBlockNumber", "");

        this.strategy.populateStep(props);
        return this.chainState.getHeadBlockNumber();
    }

    @Override
    public Block getBestBlock() {
        var headBlockNumber = this.chainState.getHeadBlockNumber();

        if (headBlockNumber == 2) {
            System.out.println("hello world");
        }

        var props = new Properties();
        props.setProperty("getBestBlock", "");
        props.setProperty("number", Long.toString(headBlockNumber));

        // this is in the case where our post-trigger number is _lower_
        // than our current number
        if (this.chainState.getHeadBlockNumber() < headBlockNumber) {
            headBlockNumber = this.chainState.getHeadBlockNumber();
        }

        this.strategy.populateStep(props);
        return this.chainState.getBlock(headBlockNumber);
    }
}
