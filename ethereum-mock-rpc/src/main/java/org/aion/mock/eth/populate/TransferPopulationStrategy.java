package org.aion.mock.eth.populate;

import org.aion.abi.AbiEncoder;
import org.aion.abi.Bytes32FVM;
import org.aion.abi.Uint256FVM;
import org.aion.mock.eth.core.MockTransaction;
import org.aion.mock.eth.state.ChainState;
import org.ethereum.core.Transaction;
import org.ethereum.core.TransactionReceipt;
import org.ethereum.db.ByteArrayWrapper;

import javax.annotation.Nonnull;
import java.math.BigInteger;
import java.util.List;

public abstract class TransferPopulationStrategy extends PopulationStrategy {

    protected static final String BURN_SIGNATURE = "burn(bytes32,uint256)";

    protected final List<TransferEvent> transferEventList;

    public TransferPopulationStrategy(@Nonnull final ChainState state,
                                      @Nonnull final List<TransferEvent> transferEventList) {
        super(state);
        this.transferEventList = transferEventList;
    }
    
    protected PostTransactionExecution execute(TransferEvent event) {
        var payload = new AbiEncoder(BURN_SIGNATURE, new Bytes32FVM(
                new ByteArrayWrapper(event.recipient)),
                new Uint256FVM(new ByteArrayWrapper(event.amount.toByteArray()))
        ).encodeBytes();
        var transaction = new MockTransaction();
    }

    public static class TransferEvent {
        public final byte[] recipient;
        public final BigInteger amount;
        public final long blockNumber;

        public TransferEvent(@Nonnull final byte[] recipient,
                             @Nonnull final BigInteger amount,
                             @Nonnull final long blockNumber) {
            this.recipient = recipient;
            this.amount = amount;
            this.blockNumber = blockNumber;
        }
    }

    protected static class PostTransactionExecution {
        public final Transaction transaction;
        public final TransactionReceipt receipt;

        public PostTransactionExecution(@Nonnull final Transaction transaction,
                                        @Nonnull final TransactionReceipt receipt) {
            this.transaction = transaction;
            this.receipt = receipt;
        }
    }
}
