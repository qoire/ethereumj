package org.aion.mock.eth.populate.rules;

import org.aion.mock.eth.core.BlockConstructor;
import org.aion.mock.eth.state.ChainState;
import org.ethereum.core.Block;
import org.ethereum.core.TransactionInfo;

import java.util.List;
import java.util.Properties;

/**
 * Define rule as an action on a blockchain, rules are applied
 * to the state per execution
 */
public abstract class AbstractRule {

    // some rules may need to implement a start method (not all)
    public abstract void start();

    public abstract void apply(ChainState state);

    public abstract void applyStep(ChainState state, Properties props);
}
