package org.aion.mock.eth.populate.pipeline;

import lombok.AllArgsConstructor;
import org.aion.mock.eth.core.BlockConstructor;
import org.aion.mock.eth.populate.ExecutionUtilities;
import org.aion.util.DeterministicRandomGenerator;
import org.ethereum.core.Bloom;
import org.ethereum.core.TransactionReceipt;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class RandomTransfer implements BlockPipelineElement {

    private long amount;
    private byte[] contractAddress;

    public BlockItem process(BlockItem item) {
        assert item.getBlock().getTransactionsList().size() == item.getReceipts().size();

        if (item.getReceipts().size() >= this.amount)
            return item;

        List<TransactionReceipt> receipts = item.getReceipts();
        List<TransactionReceipt> newReceipts = new ArrayList<>();
        for (int i = 0; i < amount - receipts.size(); i++) {
            byte[] ethereumSenderAddress = DeterministicRandomGenerator.getEthereumAddress();
            byte[] ethereumDestinationAddress = DeterministicRandomGenerator.getEthereumAddress();
            byte[] aionRecipientAddress = DeterministicRandomGenerator.getAionAddress();

            // pseudo execute the transactions
            ExecutionUtilities.TransferEvent event = new ExecutionUtilities.TransferEvent(
                    "random" + i,
                    DeterministicRandomGenerator.getEthereumAddress(),
                    aionRecipientAddress,
                    BigInteger.ONE,
                    item.getBlock().getNumber());

            // add to receipts
            var executed = ExecutionUtilities.executeTransferPayload(
                    this.contractAddress,
                    ethereumSenderAddress,
                    ethereumDestinationAddress,
                    Collections.singletonList(event));

            receipts.add(executed.getReceipt());
            newReceipts.add(executed.getReceipt());
        }

        var receiptsTrie = BlockConstructor.calcReceiptsTrie(receipts);
        var transactions = receipts.stream().map(TransactionReceipt::getTransaction).collect(Collectors.toList());
        var txTrie = BlockConstructor.calcTxTrie(transactions);

        final var bloom = new Bloom();
        for (var r : receipts) {
            bloom.or(r.getBloomFilter());
        }

        // update the block contents to grab new hash
        item.getBlock().updateTransactionContents(transactions, txTrie, receiptsTrie, bloom.getData());
        item.getReceipts().addAll(newReceipts);
        return item;
    }
}
