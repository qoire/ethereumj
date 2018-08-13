package org.aion.mock.eth.populate.rules;

import org.aion.mock.eth.core.BlockConstructor;
import org.aion.mock.eth.populate.ExecutionUtilities;
import org.aion.mock.eth.state.ChainState;
import org.aion.util.MockAddressGenerator;
import org.ethereum.core.Block;
import org.ethereum.core.TransactionInfo;
import org.ethereum.core.TransactionReceipt;

import javax.annotation.Nonnull;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This rule will generate random send transactions, and place
 * them into the block at initialization
 */
public class RandomPopulationRule extends AbstractRule {

    private final int amount;
    private final byte[] contractAddress;

    public RandomPopulationRule(final int amount,
                                @Nonnull final byte[] contractAddress) {
        this.amount = amount;
        this.contractAddress = contractAddress;
    }

    @Override
    public void start() {
        // do nothing
    }

    @Override
    public void build(@Nonnull final Block block, @Nonnull final List<TransactionInfo> infos) {
        assert block.getTransactionsList().size() == infos.size();

        if (infos.size() >= this.amount)
            return;

        List<TransactionReceipt> receipts = infos.stream().map(TransactionInfo::getReceipt).collect(Collectors.toList());
        List<TransactionReceipt> newReceipts = new ArrayList<>();
        for (int i = 0; i < amount - infos.size(); i++) {
            byte[] recipientAddress = MockAddressGenerator.getAionAddress();
            byte[] senderAddress = MockAddressGenerator.getEthereumAddress();

            // pseudo execute the transactions
            ExecutionUtilities.TransferEvent event = new ExecutionUtilities.TransferEvent(
                    recipientAddress, BigInteger.ONE, block.getNumber());

            // add to receipts
            var executed = ExecutionUtilities.executeTransferPayload(contractAddress, Collections.singletonList(event), senderAddress);
            receipts.add(executed.getReceipt());
            newReceipts.add(executed.getReceipt());
        }

        var receiptsTrie = BlockConstructor.calcReceiptsTrie(receipts);
        var transactions = receipts.stream().map(TransactionReceipt::getTransaction).collect(Collectors.toList());
        var txTrie = BlockConstructor.calcTxTrie(transactions);

        // update the block contents to grab new hash
        block.updateTransactionContents(transactions, txTrie, receiptsTrie);
        byte[] newHash = block.getHash();

        for (var r : newReceipts) {
            infos.add(new TransactionInfo(r, newHash, 0));
        }
    }

    @Override
    public void applyStep(ChainState state) {
        // do nothing here
    }
}
