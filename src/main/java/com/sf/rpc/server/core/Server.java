package com.sf.rpc.server.core;

/**
 * @author : 周川
 * @version :
 **/
public abstract class Server {
    /**
     * 开启
     * @throws Exception 异常
     */
    public abstract void start() throws Exception;

    /**
     * 关闭
     * @throws Exception 异常
     */
    public abstract void stop() throws Exception;

}
