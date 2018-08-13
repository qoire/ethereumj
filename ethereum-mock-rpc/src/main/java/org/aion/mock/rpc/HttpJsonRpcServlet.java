package org.aion.mock.rpc;

import com.googlecode.jsonrpc4j.JsonRpcServer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HttpJsonRpcServlet extends HttpServlet {

    private EthJsonRpcImpl ethJsonRpcImpl;
    private JsonRpcServer jsonRpcServer;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        jsonRpcServer.handle(req, resp);
    }

    @Override
    public void init(ServletConfig config) {
        this.ethJsonRpcImpl = (EthJsonRpcImpl) getServletContext().getAttribute("ethJsonRpcImpl");
        this.jsonRpcServer = new JsonRpcServer(this.ethJsonRpcImpl, EthJsonRpcImpl.class);
    }
}
