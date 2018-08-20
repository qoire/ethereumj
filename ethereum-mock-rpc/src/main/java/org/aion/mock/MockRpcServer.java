package org.aion.mock;

import lombok.extern.slf4j.Slf4j;
import static org.aion.util.CollectionsUtility.*;
import org.aion.mock.config.ConfigLoader;
import org.aion.mock.config.ExitCode;
import org.aion.mock.config.ServerConfig;
import org.aion.mock.eth.ChainFacade;
import org.aion.mock.eth.DefaultChainFacade;
import org.aion.mock.eth.populate.ExecutionUtilities;
import org.aion.mock.eth.populate.PopulationStrategy;
import org.aion.mock.eth.populate.PopulationEngine;
import org.aion.mock.eth.populate.base.ForkEvent;
import org.aion.mock.eth.populate.pipeline.RandomTransfer;
import org.aion.mock.eth.populate.pipeline.UserTransfer;
import org.aion.mock.eth.populate.rules.AbstractRule;
import org.aion.mock.eth.populate.rules.ForkBuilderRule;
import org.aion.mock.eth.populate.rules.TickRule;
import org.aion.mock.eth.state.ChainState;
import org.aion.mock.rpc.AddContentTypeFilter;
import org.aion.mock.rpc.HttpJsonRpcServlet;
import org.aion.util.CollectionsUtility;
import org.aion.util.MockAddressGenerator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.ethereum.util.ByteUtil;

import javax.annotation.Nonnull;
import javax.servlet.DispatcherType;
import java.math.BigInteger;
import java.util.*;

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

        log.info("best blockHash: " + ByteUtil.toHexString(facade.getBestBlock().getHash()));

        try {
            server.start();
            server.join();
        } catch (Exception e) {
            log.error("Top level exception caught from server", e);
        }
    }

    private static ChainFacade generateChainFacade(ServerConfig config) {
        var state = new ChainState();

        var randomTransferGen = new RandomTransfer(config.getThroughput(),
                config.getContractAddressBytes());

        // generate all fork events
        var forkEvents = generateForkEvents(config);

        List<AbstractRule> rules = new ArrayList<>();

        // generate a default fork event
        var forkBuilder = new ForkBuilderRule(state, forkEvents);
        // attach UserTransfer pipeline element (for generating transfers)
        forkBuilder.attach(new UserTransfer(config.getContractAddressBytes(), forkEvents));

        // add forkBuilder rule (always necessary)
        rules.add(forkBuilder);

        if (config.getMode().contains("ticking")) {
            rules.add(new TickRule(config.getBlockTime(), config.getForks().get("main").getStartNumber()));
        }

        if (config.getMode().contains("throughput")) {
            // attach random transaction generation element
            forkBuilder.attach(randomTransferGen);
        }

        PopulationStrategy strategy = PopulationEngine.builder()
                .state(state)
                .specialRules(rules)
                .build();
        return new DefaultChainFacade(strategy, state);
    }

    /**
     * Generate a handy-dandy jetty server, with a JsonRpc handler, and a filter
     * for detecting proper header configurations.
     *
     * @param facade instance of chainfacade to query blockchain information from
     * @param config server config
     * @return {@code JSON-RPC server} ready to start
     */
    private static Server generateJettyServer(ChainFacade facade, ServerConfig config) {
        Server server = new Server(config.getPort());
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.addFilter(AddContentTypeFilter.class, "/", EnumSet.of(DispatcherType.REQUEST));
        context.addServlet(new ServletHolder(new HttpJsonRpcServlet(facade)), "/");
        server.setHandler(context);
        return server;
    }

    /**
     * Simple mapping functions, here we are mapping between a relationship between
     * two sets {@link org.aion.mock.config.ServerConfig.Forks} and {@link org.aion.mock.config.ServerConfig.Transfers}
     * where forks reference transfers via a string identifier.
     * The relationship is turned into a single Set (actually a Map) where each item in the Set
     * represents a fork, that contains another Set of transfers.
     *
     * @param config server configuration
     * @return {@code map/set} representing the various forks and their desired transfers
     */
    private static Map<String, ForkEvent> generateForkEvents(@Nonnull final ServerConfig config) {
        Map<String, ForkEvent> forkEventMap = new HashMap<>();
        for (var fork : config.getForks().entrySet()) {
            var forkEventBuilder = ForkEvent.builder();
            forkEventBuilder
                    .forkName(fork.getKey())
                    .forkStartBlockNumber(fork.getValue().getStartNumber())
                    .forkEndBlockNumber(fork.getValue().getEndNumber())
                    .forkTriggerNumber(fork.getValue().getTriggerNumber())
                    .forkPostTriggerNumber(fork.getValue().getPostTriggerNumber());

            if (fork.getValue().getTransfers() != null) {
                // find the intersection between a fork and transfers
                var forkTransferEvents = intersection(
                        config.getTransfers().entrySet(),
                        fork.getValue().getTransfers().entrySet(),
                        (t, f) -> t.getKey().equals(f.getKey()));

                for (var i : forkTransferEvents) {
                    forkEventBuilder.forkTransferEvent(toTransferEvent(i.getX(), i.getY().getValue()));
                }

                ForkEvent event = forkEventBuilder.build();
                event.sortTransfers();
            }

            forkEventMap.put(fork.getKey(), forkEventBuilder.build());
        }
        return forkEventMap;
    }

    private static ExecutionUtilities.TransferEvent
    toTransferEvent(@Nonnull final Map.Entry<String, ServerConfig.Transfers> t,
                    final long blockNumber) {
        return new ExecutionUtilities.TransferEvent(t.getKey(),
                ByteUtil.hexStringToBytes(t.getValue().getFrom()),
                ByteUtil.hexStringToBytes(t.getValue().getTo()),
                BigInteger.valueOf(t.getValue().getAmount()),
                blockNumber);
    }
}
