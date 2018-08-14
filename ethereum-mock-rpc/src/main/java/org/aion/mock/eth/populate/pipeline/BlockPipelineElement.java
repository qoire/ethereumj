package org.aion.mock.eth.populate.pipeline;

public interface BlockPipelineElement {
    BlockItem process(BlockItem item);
}
