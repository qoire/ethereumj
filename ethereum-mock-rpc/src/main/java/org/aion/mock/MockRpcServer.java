package org.aion.mock;

import lombok.extern.slf4j.Slf4j;
import org.aion.mock.config.ConfigLoader;
import org.aion.mock.config.ExitCode;
import org.aion.mock.config.ServerConfig;
import org.aion.mock.eth.ChainFacade;
import org.aion.mock.eth.DefaultChainFacade;
import org.aion.mock.eth.populate.PopulationStrategy;
import org.aion.mock.eth.populate.PopulationEngine;
import org.aion.mock.eth.populate.pipeline.RandomTransfer;
import org.aion.mock.eth.populate.rules.ForkBuilderRule;
import org.aion.mock.eth.state.ChainState;
import org.aion.mock.rpc.AddContentTypeFilter;
import org.aion.mock.rpc.HttpJsonRpcServlet;
import org.aion.util.MockAddressGenerator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.DispatcherType;
import java.util.Collections;
import java.util.EnumSet;

@Slf4j
public class MockRpcServer {

    // some invariants for now, needs to change into config file parameters later
    static final byte[] CONTRACT_ADDRESS = MockAddressGenerator.getEthereumAddress();

    public static void main(String[] args) {
        ServerConfig config = ConfigLoader.load();

        if (config == null) {
            log.error("error loading configuration file (config.yaml)");
            System.exit(ExitCode.CONFIG_ERR.code());
        }

        log.info("loaded config");
        log.info(config.toString());

        ChainFacade facade = generateChainFacade(config);
        Server server = generateJettyServer(facade, config);

        try {
            server.start();
            server.join();
        } catch (Exception e) {
            log.error("Top level exception caught from server", e);
        }
    }

    private static ChainFacade generateChainFacade(ServerConfig config) {
        var state = new ChainState();
        var randomTransferGen = new RandomTransfer(100, CONTRACT_ADDRESS);

        var forkEvent = ForkBuilderRule.ForkEvent.builder()
                .forkStartBlockNumber(0)
                .forkEndBlockNumber(100)
                .forkName("main")
                .forkTransferEvents(Collections.emptyList())
                .build();

        // generate a default fork event
        var forkBuilder = new ForkBuilderRule(state, Collections.singletonList(forkEvent));
        forkBuilder.attach(randomTransferGen);

        PopulationStrategy strategy = PopulationEngine.builder()
                .startNumber(0)
                .endNumber(128)
                .state(state)
                .specialRules(Collections.singletonList(forkBuilder))
                .build();
        return new DefaultChainFacade(strategy, state);
    }

    private static Server generateJettyServer(ChainFacade facade, ServerConfig config) {
        Server server = new Server(config.getPort());
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.addFilter(AddContentTypeFilter.class, "/", EnumSet.of(DispatcherType.REQUEST));
        context.addServlet(new ServletHolder(new HttpJsonRpcServlet(facade)), "/");
        server.setHandler(context);
        return server;
    }
}
