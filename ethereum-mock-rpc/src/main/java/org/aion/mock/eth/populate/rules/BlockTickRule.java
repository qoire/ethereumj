package org.aion.mock.eth.populate.rules;

import org.aion.mock.eth.core.BlockConstructor;
import org.aion.mock.eth.state.ChainState;
import org.ethereum.core.Block;
import org.ethereum.core.TransactionInfo;

import javax.annotation.Nonnull;
import java.util.List;

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
    public void build(@Nonnull final Block block, List<TransactionInfo> infos) {
        
    }

    @Override
    public void applyStep(ChainState state) {
        int diff = (int) (((System.currentTimeMillis() / 1000) - timestamp) / 10);
        state.setHeadBlockNumber(Math.min(this.startBlockNumber + diff, state.getChainBlockNumber()));
    }
}
