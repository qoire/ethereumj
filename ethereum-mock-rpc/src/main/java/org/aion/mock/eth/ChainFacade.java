package org.aion.mock.eth;

import org.ethereum.core.Block;
import org.ethereum.core.Transaction;
import org.ethereum.core.TransactionInfo;
import org.ethereum.core.TransactionReceipt;

/**
 * ChainFacade refers to a facade, or variant of blockchain mocks
 * that only simulates the surface level chain mechanics <b>without</b>
 * simulation of the state.
 *
 * These facades are able to:
 *
 * <li>
 *     <ul>Simulate the block structure</ul>
 *     <ul>Simulate the transaction structure as it relates to blocks </ul>
 *     <ul>Simulate the receipt structure, as it relates to transactions and blocks</ul>
 * </li>
 *
 * This facade however is not expected, and will not simulate the state
 * of the world.
 *
 */
public interface ChainFacade {

    long getBlockNumber();

    Block getBlockByNumber(long number);

    Block getBlockByHash(byte[] blockHash);

    Transaction getTransactionByHash(byte[] transactionHash);

    TransactionReceipt getTransactionReceiptByHash(byte[] transactionHash);

    TransactionInfo getTransactionInfo(byte[] transactionHash);

    Block getBestBlock();
}
