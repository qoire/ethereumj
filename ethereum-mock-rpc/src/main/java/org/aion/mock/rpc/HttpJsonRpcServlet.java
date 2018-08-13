package org.aion.mock.rpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import org.aion.mock.eth.ChainFacade;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HttpJsonRpcServlet extends HttpServlet {

    private ChainFacade chainFacade;
    private EthJsonRpcImpl ethJsonRpcImpl;
    private JsonRpcServer jsonRpcServer;

    public HttpJsonRpcServlet(ChainFacade chainFacade) {
        this.chainFacade = chainFacade;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        jsonRpcServer.handle(req, resp);
    }

    @Override
    public void init(ServletConfig config) {
        this.ethJsonRpcImpl = new EthJsonRpcImpl(this.chainFacade);
        this.jsonRpcServer = new JsonRpcServer(new ObjectMapper(), this.ethJsonRpcImpl, EthJsonRpcImpl.class);
    }
}
