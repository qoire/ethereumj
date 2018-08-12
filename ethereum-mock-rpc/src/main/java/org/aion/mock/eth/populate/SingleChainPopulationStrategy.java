package org.aion.mock.eth.populate;

import org.aion.mock.eth.state.ChainState;

import java.util.List;
import java.util.Properties;

public class SingleChainPopulationStrategy extends TransferPopulationStrategy {

    public SingleChainPopulationStrategy(ChainState state, List<TransferEvent> events) {
        super(state, events);
    }

    @Override
    public void populateInitialInternal() {

    }

    @Override
    public void populateStep(Properties props) {

    }
}
