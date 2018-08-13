package org.aion.mock.eth.populate.rules;

import org.aion.mock.eth.core.BlockConstructor;
import org.aion.mock.eth.state.ChainState;
import org.ethereum.core.Block;
import org.ethereum.core.TransactionInfo;

import java.util.List;

/**
 * Define rule as an action on a blockchain, rules are applied
 * to the state per execution
 */
public abstract class AbstractRule {

    // some rules may need to implement a start method (not all)
    public abstract void start();

    public abstract void build(Block block, List<TransactionInfo> infos);

    public abstract void applyStep(ChainState state);
}
