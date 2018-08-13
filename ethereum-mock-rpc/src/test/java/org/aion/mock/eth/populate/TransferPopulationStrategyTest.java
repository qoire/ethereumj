package org.aion.mock.eth.populate;

import org.junit.Test;

public class TransferPopulationStrategyTest {
    @Test
    public void testBuilder() {
        var builder = TransferPopulationStrategy.builder();
        System.out.println(builder.toString());
    }
}
