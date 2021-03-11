package com.sf.rpc.client.handler;

/**
 * @Author: 01397429 周川
 * @Description:
 * @Date: create in 2021/3/5 10:26
 */
public interface AsyncRPCCallback {
    void success(Object result);

    void fail(Exception e);
}
