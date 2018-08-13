package org.aion.mock.eth.populate.rules;

import org.aion.mock.eth.state.ChainState;

import javax.annotation.Nonnull;

public class BlockTickRule extends AbstractRule {

    private final long startBlockNumber;
    private final long stride;

    public BlockTickRule(@Nonnull final long startNumber,
                         @Nonnull final long stride) {
        this.startBlockNumber = startNumber;
        this.stride = stride;
    }

    long timestamp = 0L;

    public void start() {
        timestamp = System.currentTimeMillis() / 1000;
    }

    @Override
    public void apply(ChainState state) {
        // do nothing, this rule is only for ticking the block number up
    }

    @Override
    public void applyStep(ChainState state) {
        int diff = (int) (((System.currentTimeMillis() / 1000) - timestamp) / 10);
        state.setHeadBlockNumber(Math.min(this.startBlockNumber + diff, state.getChainBlockNumber()));
    }
}
