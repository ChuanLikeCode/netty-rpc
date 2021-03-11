package com.sf.rpc.client.handler;

/**
 * @author :  周川
 * @version :
 * */
public interface AsyncRPCCallback {
    void success(Object result);

    void fail(Exception e);
}
