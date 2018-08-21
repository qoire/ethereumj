package org.aion.mock.eth.populate.rules;

import lombok.extern.slf4j.Slf4j;
import org.aion.mock.eth.core.BlockConstructor;
import org.aion.mock.eth.populate.base.ForkEvent;
import org.aion.mock.eth.populate.pipeline.BlockItem;
import org.aion.mock.eth.populate.pipeline.BlockPipelineElement;
import org.aion.mock.eth.state.ChainState;
import org.aion.util.DeterministicRandomGenerator;
import org.ethereum.core.*;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Rule that forks between different block numbers, before which a blockchain
 * will be on a certain fork of the chain, and afterwards another.
 *
 * This is the most complex rule, as it has to keep track of a set of transactions
 * to be placed into the blockchain at correct moments.
 */
@Slf4j
@ThreadSafe
public class ForkBuilderRule extends AbstractRule {

    @GuardedBy("this")
    private List<BlockPipelineElement> bpe = new ArrayList<>();

    @GuardedBy("this")
    private final Map<String, ForkEvent> forkEvents;

    private final ChainState state;

    private long lastBlockNumber;

    public ForkBuilderRule(ChainState state, Map<String, ForkEvent> forkEvents) {
        this.state = state;
        this.forkEvents = forkEvents;
        this.lastBlockNumber = 0;
        checkDuplicates();
    }

    @GuardedBy("this")
    private void checkMain() {
        if (this.forkEvents.keySet().stream().filter(s -> s.equals("main")).count() != 1) {
            throw new RuntimeException("invalid main fork count");
        }
    }

    @GuardedBy("this")
    private void checkDuplicates() {
        {
            List<Long> duplicates = this.forkEvents.values()
                    .stream().map(ForkEvent::getForkTriggerNumber)
                    .collect(Collectors.toList());
            duplicates.sort(Long::compareTo);

            var lastValue = -1l;
            for (var l : duplicates) {
                if (lastValue == l) {
                    throw new RuntimeException("found duplicate fork end block number");
                }
                lastValue = l;
            }
        }
    }

    /**
     * ForkBuilder rule introduces the concept of a pipeline, all blocks
     * constructed go through a pipeline system
     *
     * @param bpe
     */
    public synchronized void attach(BlockPipelineElement bpe) {
        this.bpe.add(bpe);
        log.debug("attached pipeline element class {} at position {}", bpe.getClass().toString(), this.bpe.size() - 1);
    }

    @Override
    public synchronized void start() {
        // do nothing, we initialize a starting point anyways
    }

    /**
     * Here we construct all possible forks that we know about. For each
     * fork, we attach a index to them, so we can identify them later
     * @param state
     */
    @Override
    public synchronized void apply(ChainState state) {
        // validate input configurations
        validateInputs();
        generateIndices();

        // grab the ranges
        long min = Collections.min(this.forkEvents.values(), new ForkEventStartBlockComparator()).getForkStartBlockNumber();
        long max = Collections.max(this.forkEvents.values(), new ForkEventEndBlockComparator()).getForkEndBlockNumber();

        log.info("building {} forks between block number {} to {}", this.forkEvents.keySet().size(), min, max);

        Map<String, byte[]> parentHashes = new HashMap<>();
        for (long i = min; i < max; i++) {
            for (ForkEvent event : this.forkEvents.values()) {
                if (isInRange(i, event)) {
                    var builder = constructDefault(i, event);
                    {
                        byte[] parentHash = null;
                        if ((parentHash = parentHashes.get(event.getForkName())) != null) {
                            builder.parentHash(parentHash);
                        }
                    }

                    BlockItem item = new BlockItem(event.getForkName(),
                            builder.build().buildBlock(), new ArrayList<>());

                    // run it through the block process elements
                    for (var e : this.bpe) {
                        item = e.process(item);
                    }

                    // after this point, we calculate the TransactionInfo
                    byte[] blockHash = postConstruction(item);
                    parentHashes.put(event.getForkName(), blockHash);
                }
            }
        }
        this.state.setCurrentFork("main");
    }

    @GuardedBy("this")
    private static boolean isInRange(long i, ForkEvent event) {
        return i >= event.getForkStartBlockNumber() && i <= event.getForkEndBlockNumber();
    }

    /**
     * The other responsibility for handling forks is actually forking when the
     * current block number matches a certain criteria.
     *
     * @param state
     */
    @Override
    public void applyStep(ChainState state, Properties props) {
        if (props.getProperty("number") == null) {
            // only trigger when the user stumbles on this using an API call
            return;
        }

        synchronized (this.state) {
            long requestedBlockNumber = Long.valueOf(props.getProperty("number"));
            if (requestedBlockNumber > this.state.getHeadBlockNumber()) {
                // return if the user tries to request a number that is
                // outside of the range of what we have currently exposed
                return;
            }

            List<Map.Entry<String, ForkEvent>> out = this.forkEvents.entrySet().stream()
                    .filter(e -> e.getValue().getForkTriggerNumber() == requestedBlockNumber)
                    .collect(Collectors.toList());

            if (out.size() > 1) {
                log.debug("multiple forks trigger at block, this should not happen");
                throw new RuntimeException("multiple forks trigger at this block");
            }

            if (out.isEmpty()) {
                log.debug("no trigger event at this block number, current fork: {}", this.state.getCurrentFork());
                return;
            }

            if (out.get(0).getKey().equals(this.state.getCurrentFork())) {
                log.debug("fork trigger detected for current fork {}, doing nothing", this.state.getCurrentFork());
                return;
            }

            // always get the earliest fork
            String nextForkName = out.get(0).getKey();
            ForkEvent nextForkEvent = out.get(0).getValue();
            // otherwise, apply the fork event
            log.debug("applying fork rule, forks {} => {}, requested => {}, forkRange => [{},{}]",
                    this.state.getCurrentFork(),
                    nextForkName,
                    requestedBlockNumber,
                    nextForkEvent.getForkStartBlockNumber(),
                    nextForkEvent.getForkEndBlockNumber());

            this.state.setCurrentFork(nextForkName);
            this.state.setHeadBlockNumber(nextForkEvent.getForkPostTriggerNumber());
        }
    }

    @GuardedBy("this")
    private void validateInputs() {
        // validate that correct input is available
        long mainChain = this.forkEvents
                .values()
                .stream()
                .filter(f -> f.getForkName().equals("main"))
                .count();

        if (mainChain != 1) {
            throw new RuleException("cannot find main fork defined");
        }

        long invalidChainDefinitions = this.forkEvents
                .values()
                .stream()
                .filter(f -> {
                    if (f.getForkEndBlockNumber() < f.getForkStartBlockNumber()) {
                        log.error("configuration for chain: " + f.getForkName() + " endNumber < startNumber");
                        return true;
                    }
                    var count = f.getForkTransferEvents().stream()
                            .filter(e -> {
                                if (e.getBlockNumber() > f.getForkEndBlockNumber() ||
                                        e.getBlockNumber() < f.getForkStartBlockNumber()) {
                                    log.error("configuration for chain: " + f.getForkName() + " invalid transfer range");
                                    return true;
                                }
                                return false;
                            }).count();
                    return count > 0;
                }).count();

        if (invalidChainDefinitions > 0)
            throw new RuleException("inputs violated some ForkBuilderRule invariants");
    }

    private BlockConstructor.BlockConstructorBuilder constructDefault(long blockNumber, ForkEvent event) {
        var builder = BlockConstructor.builder();
        builder.number(blockNumber)
                .difficulty(event.getInitialDifficulty()
                        .add(BigInteger.valueOf(blockNumber))
                        .subtract(BigInteger.valueOf(event.getForkStartBlockNumber()))
                        .toByteArray())
                .nonce(DeterministicRandomGenerator.getBytes32());
        return builder;
    }

    private byte[] postConstruction(final BlockItem item) {
        List<Transaction> transactions = item.getReceipts()
                .stream()
                .map(TransactionReceipt::getTransaction)
                .collect(Collectors.toList());

        var txTrieRoot = BlockConstructor.calcTxTrie(transactions);
        var receiptRoot = BlockConstructor.calcReceiptsTrie(item.getReceipts());

        final var bloom = new Bloom();
        for (var r : item.getReceipts()) {
            bloom.or(r.getBloomFilter());
        }

        item.getBlock().updateTransactionContents(transactions, txTrieRoot, receiptRoot, bloom.getData());
        var blockHash = item.getBlock().getHash();

        List<TransactionInfo> infos = item.getReceipts()
                .stream()
                .map(r -> new TransactionInfo(r, blockHash, 0))
                .collect(Collectors.toList());

        // store into state
        applyStateUpdate(item.getBlock(), infos, item.getFork());
        return blockHash;
    }

    private void applyStateUpdate(final Block block,
                                  final List<TransactionInfo> infos,
                                  final String forkName) {
        this.state.addBlock(block, infos, forkName);
    }

    private void generateIndices() {

    }

    protected static class ForkEventStartBlockComparator implements Comparator<ForkEvent> {
        @Override
        public int compare(@Nonnull final ForkEvent o1,
                           @Nonnull final ForkEvent o2) {
            if (o1.getForkStartBlockNumber() < o2.getForkStartBlockNumber())
                return -1;

            if (o1.getForkStartBlockNumber() > o2.getForkStartBlockNumber())
                return 1;

            return 0;
        }

        // TODO: this is most likely incorrect
        @Override
        public boolean equals(Object other) {
            return System.identityHashCode(this) == System.identityHashCode(other);
        }
    }

    protected static class ForkEventEndBlockComparator implements Comparator<ForkEvent> {
        @Override
        public int compare(@Nonnull final ForkEvent o1,
                           @Nonnull final ForkEvent o2) {
            if (o1.getForkEndBlockNumber() < o2.getForkEndBlockNumber())
                return -1;

            if (o1.getForkEndBlockNumber() > o2.getForkEndBlockNumber())
                return 1;

            return 0;
        }

        // TODO: this is most likely incorrect
        @Override
        public boolean equals(Object other) {
            return System.identityHashCode(this) == System.identityHashCode(other);
        }
    }
}
