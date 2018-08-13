package org.aion.mock.eth;

import lombok.extern.slf4j.Slf4j;
import org.aion.mock.eth.populate.PopulationStrategy;
import org.aion.mock.eth.populate.TransferPopulationStrategy;
import org.aion.mock.eth.populate.rules.RandomPopulationRule;
import org.aion.mock.eth.state.ChainState;
import org.aion.mock.rpc.HttpJsonRpcServlet;
import org.aion.util.MockAddressGenerator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.util.Collections;

@Slf4j
public class MockRpcServer {

    // some invariants for now, needs to change into config file parameters later
    static final byte[] CONTRACT_ADDRESS = MockAddressGenerator.getEthereumAddress();

    public static void main(String[] args) {
        ChainFacade facade = generateChainFacade();
        Server server = generateJettyServer(facade);

        try {
            server.start();
            server.join();
        } catch (Exception e) {
            log.error("Top level exception caught from server", e);
        }
    }

    private static ChainFacade generateChainFacade() {
        var state = new ChainState();
        PopulationStrategy strategy = TransferPopulationStrategy.builder()
                .startNumber(0)
                .endNumber(128)
                .state(state)
                .specialRules(Collections.singletonList(new RandomPopulationRule(100, CONTRACT_ADDRESS)))
                .build();
        return new DefaultChainFacade(strategy, state);
    }

    private static Server generateJettyServer(ChainFacade facade) {
        Server server = new Server(8545);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.addServlet(new ServletHolder(new HttpJsonRpcServlet(facade)), "/");
        server.setHandler(context);
        return server;
    }
}
