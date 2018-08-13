package org.aion.mock.eth;

import com.googlecode.jsonrpc4j.JsonRpcServer;
import org.aion.mock.rpc.EthJsonRpcImpl;
import org.aion.mock.rpc.JsonRpc;

public class MockRpcServer {
    public static void main(String[] args) {

        var rpcHandler = new EthJsonRpcImpl();
        var server = new JsonRpcServer(rpcHandler, JsonRpc.class);
    }
}
