package org.aion.mock.eth.populate;

import org.aion.abi.AbiEncoder;
import org.aion.abi.Bytes32FVM;
import org.aion.abi.Uint256FVM;
import org.aion.mock.eth.core.MockTransaction;
import org.aion.mock.eth.populate.rules.AbstractRule;
import org.aion.mock.eth.state.ChainState;
import org.aion.util.MockAddressGenerator;
import org.ethereum.core.Bloom;
import org.ethereum.core.Transaction;
import org.ethereum.core.TransactionReceipt;
import org.ethereum.db.ByteArrayWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.List;
import java.util.Properties;

import static org.aion.util.MockAddressGenerator.getEthereumAddress;

public class TransferPopulationStrategy extends PopulationStrategy {

    protected final List<ExecutionUtilities.TransferEvent> transferEventList;
    protected final List<AbstractRule> specialRules;

    public TransferPopulationStrategy(@Nonnull final ChainState state,
                                      @Nonnull final List<ExecutionUtilities.TransferEvent> transferEventList,
                                      @Nonnull final List<AbstractRule> specialRules) {
        super(state);
        this.transferEventList = transferEventList;
        this.specialRules = specialRules;
    }

    protected void populate() {

    }

    @Override
    public void populateInitialInternal() {
        populate();
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
