package org.aion.mock.eth.populate.rules;

import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.aion.mock.eth.state.ChainState;

/**
 * Ticks until we reach the chain head
 */
@Slf4j
public class TickRule extends AbstractRule {

    private final Stopwatch watch = Stopwatch.createUnstarted();
    private final long blockTime;
    private final long startingBlockNumber;

    public TickRule(final long blockTime,
                    final long startingBlockNumber) {
        this.blockTime = blockTime;
        this.startingBlockNumber = startingBlockNumber;
    }

    @Override
    public void start() {

    }

    @Override
    public void apply(ChainState state) {
        watch.start();
    }

    @Override
    public void applyStep(ChainState state) {
        synchronized (state) {
            var current = state.getHeadBlockNumber();
            var forkMaxBlockNumber = state.getCurrentForkMax();

            /*
             * This is a case we hit if we are currently on a chain that has "ended", but do not switch
             * (since that is done manually). In this case, we effectively have to tread the current head
             * as the head. Assuming this is the only process that "ticks".
             */
            if (state.getBlock(current + 1) == null)
                return;
            var newNum = Math.min((this.watch.elapsed().getSeconds() / this.blockTime) + startingBlockNumber, forkMaxBlockNumber);
            state.setHeadBlockNumber(newNum);
            if (newNum > current) {
                // helpful log for the user
                log.info("applied tick, new HEAD block number {}, hash {}", newNum, state.getBlock(newNum).getHash());
            }
        }
    }
}
