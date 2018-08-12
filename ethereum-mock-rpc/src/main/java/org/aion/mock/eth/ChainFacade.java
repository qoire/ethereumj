package org.aion.mock.eth;

import org.ethereum.core.Block;
import org.ethereum.core.Transaction;
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
public abstract class ChainFacade {

    public Block getBlockByNumber(long number);

    public Block getBlockByHash(byte[] blockHash);

    public Transaction getTransactionByNumber(long number);

    public Transaction getTransactionByHash(byte[] transactionHash);

    public TransactionReceipt getTransactionReceiptByHash(byte[] transactionHash);

    public abstract Block
}
