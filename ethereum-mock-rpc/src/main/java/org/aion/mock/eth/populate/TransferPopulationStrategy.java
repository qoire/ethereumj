package org.aion.mock.eth.populate;

import lombok.Builder;
import org.aion.mock.eth.core.MockBlock;
import org.aion.mock.eth.populate.rules.AbstractRule;
import org.aion.mock.eth.state.ChainState;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class TransferPopulationStrategy extends PopulationStrategy {

    protected final List<ExecutionUtilities.TransferEvent> transferEventList;
    protected final List<AbstractRule> specialRules;
    protected final Integer startNumber;
    protected final Integer endNumber;

    @Builder
    private TransferPopulationStrategy(@Nonnull final ChainState state,
                                       @Nonnull final Integer startNumber,
                                       @Nonnull final Integer endNumber,
                                       @Nonnull final List<ExecutionUtilities.TransferEvent> transferEventList,
                                       @Nonnull final List<AbstractRule> specialRules) {
        super(state);

        assert startNumber >= 0;
        assert endNumber >= startNumber;

        this.startNumber = startNumber;
        // inclusive
        this.endNumber = endNumber;

        this.transferEventList = transferEventList;
        this.specialRules = specialRules;
    }

    protected void populateBlocksWithTransfers() {
        byte[] lastParentHash = null;
        for (int i = this.startNumber; i < this.endNumber; i++) {
            var blockBuilder = MockBlock.builder()
                    .transactionsList(Collections.emptyList());

            if (lastParentHash != null)
                blockBuilder.parentHash(lastParentHash);

            var block = blockBuilder.build();
            lastParentHash = block.getHash();
        }
    }

    @Override
    public void populateInitialInternal() {
        // define the rules for initial population
        populateBlocksWithTransfers();
        // special rules are run after the standard state is built
        for (AbstractRule rule : specialRules) {
            rule.apply(this.state);
        }
    }

    @Override
    public void populateStep(Properties props) {
        // by default, the strategy does not have any default behaviour
        for (AbstractRule rule : specialRules) {
            rule.applyStep(this.state);
        }
    }
}
