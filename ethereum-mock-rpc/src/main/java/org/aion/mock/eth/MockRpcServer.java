package org.aion.mock.eth;

import org.aion.mock.eth.populate.PopulationStrategy;
import org.aion.mock.eth.populate.TransferPopulationStrategy;
import org.aion.mock.eth.state.ChainState;
import org.eclipse.jetty.server.Server;

import java.util.Collections;

public class MockRpcServer {
    public static void main(String[] args) {
        ChainFacade facade = generateChainFacade();
    }

    private static ChainFacade generateChainFacade() {
        var state = new ChainState();
        PopulationStrategy strategy = TransferPopulationStrategy.builder()
                .startNumber(0)
                .endNumber(128)
                .specialRules(Collections.emptyList())
                .state(state)
                .build();
        return new DefaultChainFacade(strategy, state);
    }

    private static Server generateJettyServer() {

    }
}
