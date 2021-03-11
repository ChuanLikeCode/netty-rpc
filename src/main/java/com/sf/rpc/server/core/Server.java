package com.sf.rpc.server.core;

/**
 * @author: 周川
 * @description:
 * @create: 2021-03-11 20:39
 **/
public abstract class Server {
    /**
     * start server
     *
     * @param
     * @throws Exception
     */
    public abstract void start() throws Exception;

    /**
     * stop server
     *
     * @throws Exception
     */
    public abstract void stop() throws Exception;

}
