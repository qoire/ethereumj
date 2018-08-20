package org.aion.mock.eth.populate;

import lombok.Data;
import org.aion.mock.eth.core.MockTransaction;
import org.ethereum.core.Transaction;
import org.ethereum.core.TransactionReceipt;
import org.ethereum.crypto.HashUtil;
import org.ethereum.vm.DataWord;
import org.ethereum.vm.LogInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.aion.util.MockAddressGenerator.getEthereumAddress;

public class ExecutionUtilities {

    protected static final byte[] EMPTY_WORD32 = new byte[32];

    protected static final String BURN_SIGNATURE = "burn(bytes32,uint256)";

    protected static final String BURN_EVENT_SIGNATURE = "Burn(address,bytes32,uint256)";

    protected static final byte[] EMPTY = new byte[0];

    public static PostTransactionExecution
    executeTransferPayload(@Nonnull final byte[] contractAddress,
                           @Nullable byte[] ethereumSenderAddress,
                           @Nullable byte[] ethereumDestinationAddress,
                           @Nonnull final List<TransferEvent> events) {
        ethereumSenderAddress = ethereumSenderAddress == null ? getEthereumAddress() : ethereumSenderAddress;
        ethereumDestinationAddress = ethereumDestinationAddress == null ? getEthereumAddress() : ethereumDestinationAddress;

        var transaction = new MockTransaction(EMPTY,
                EMPTY,
                EMPTY,
                ethereumSenderAddress,
                ethereumDestinationAddress,
                EMPTY,
                EMPTY,
                0x0);
        var transactionReceipt = createReceipt(contractAddress, transaction, events);
        return new PostTransactionExecution(transaction, transactionReceipt);
    }

    public static TransactionReceipt createReceipt(@Nonnull final byte[] contractAddress,
                                            @Nonnull final Transaction transaction,
                                            @Nonnull final List<TransferEvent> events) {
        var receipt = new TransactionReceipt();
        receipt.setTransaction(transaction);

        // invariants
        receipt.setCumulativeGas(0);
        receipt.setTxStatus(true);
        receipt.setGasUsed(0);
        receipt.setPostTxState(EMPTY_WORD32);
        receipt.setExecutionResult(EMPTY_WORD32);

        List<LogInfo> logs = new ArrayList<>();
        for (var event : events) {
            LogInfo log = new LogInfo(
                    contractAddress,
                    Arrays.asList(
                                  new DataWord(HashUtil.sha3("Burn(address,bytes32,uint256)".getBytes())),
                                  new DataWord(transaction.getSender()),
                                  new DataWord(event.recipient)),
                    new DataWord(event.amount.toByteArray()).getData());
            logs.add(log);
        }

        receipt.setLogInfoList(logs);
        return receipt;
    }

    @Data
    public static class TransferEvent {
        private final String name;
        private final byte[] sender;
        private final byte[] recipient;
        private final BigInteger amount;
        private final long blockNumber;
    }

    @Data
    public static class PostTransactionExecution {
        public final Transaction transaction;
        public final TransactionReceipt receipt;
    }
}
