package org.aion.mock.eth.populate;

import org.aion.mock.eth.populate.pipeline.RandomTransfer;
import org.aion.mock.eth.populate.rules.ForkBuilderRule;
import org.ethereum.crypto.HashUtil;
import org.junit.Test;

public class ForkBuilderRuleTest {

    private static final byte[] dummyContractAddress = HashUtil.sha3("hello".getBytes());

    @Test
    public void testBuilder() {
        ForkBuilderRule rule = new ForkBuilderRule();
        rule.attach(new RandomTransfer(100, dummyContractAddress));
    }
}
