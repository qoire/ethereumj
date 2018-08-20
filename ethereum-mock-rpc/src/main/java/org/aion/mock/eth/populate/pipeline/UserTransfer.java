package org.aion.mock.eth.populate.pipeline;

import org.aion.mock.eth.core.BlockConstructor;
import org.aion.mock.eth.populate.ExecutionUtilities;
import org.aion.mock.eth.populate.base.ForkEvent;
import org.ethereum.core.Bloom;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

public class UserTransfer implements BlockPipelineElement {

    private final byte[] contractAddress;
    private final Map<String, Queue<ExecutionUtilities.TransferEvent>> events;

    public UserTransfer(
            @Nonnull final byte[] contractAddress,
            @Nonnull final Map<String, ForkEvent> events) {
        this.contractAddress = contractAddress;
        this.events = new HashMap<>();

        // note here: we assumed the transfer events have already been ordered
        for (Map.Entry<String, ForkEvent> e : events.entrySet()) {
            this.events.put(e.getKey(), new PriorityQueue<>(e.getValue().getForkTransferEvents()));
        }
    }

    @Override
    public synchronized BlockItem process(BlockItem item) {
        var transferEvents = events.get(item.getFork());

        // this should never be null, we should always parse this properly
        assert transferEvents != null;

        List<ExecutionUtilities.TransferEvent> executedEvents = new ArrayList<>();
        EV_LOOP:
        while(!transferEvents.isEmpty()) {
            {
                ExecutionUtilities.TransferEvent e;
                if ((e = transferEvents.peek()) != null && e.getBlockNumber() == item.getBlock().getNumber()) {
                    executedEvents.add(e);
                    // remove item from tip
                    transferEvents.poll();
                } else {
                    // otherwise we are either too high or too low, break
                    break EV_LOOP;
                }
            }
        }

        if (executedEvents.isEmpty())
            return item;

        List<ExecutionUtilities.PostTransactionExecution> results = new ArrayList<>();
        for (var e : executedEvents) {
            var postExecutionResults = ExecutionUtilities.executeTransferPayload(
                    this.contractAddress,
                    e.getSender(),
                    this.contractAddress,
                    Collections.singletonList(e)
            );
            results.add(postExecutionResults);
        }

        // TODO: this can be nicer

        // update transactions
        item.getBlock().getTransactionsList().addAll(results.stream()
                .map(ExecutionUtilities.PostTransactionExecution::getTransaction)
                .collect(Collectors.toList()));

        // update receipts
        item.getReceipts().addAll(results.stream()
                .map(ExecutionUtilities.PostTransactionExecution::getReceipt)
                .collect(Collectors.toList()));

        final var bloom = new Bloom();
        for (var r : item.getReceipts()) {
            bloom.or(r.getBloomFilter());
        }

        // update transactions
        item.getBlock().updateTransactionContents(item.getBlock().getTransactionsList(),
                BlockConstructor.calcTxTrie(item.getBlock().getTransactionsList()),
                BlockConstructor.calcReceiptsTrie(item.getReceipts()),
                bloom.getData());

        return item;
    }
}
