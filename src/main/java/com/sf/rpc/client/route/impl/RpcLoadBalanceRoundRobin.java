package com.sf.rpc.client.route.impl;

import com.sf.rpc.client.handler.RpcClientHandler;
import com.sf.rpc.client.route.RpcLoadBalance;
import com.sf.rpc.common.protocol.RpcProtocol;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询调度算法(Round-Robin Scheduling)
 * 轮询调度算法的原理是每一次把来自用户的请求轮流分配给内部中的服务器，从1开始，直到N(内部服务器个数)，然后重新开始循环。
 * 算法的优点是其简洁性，它无需记录当前所有连接的状态，所以它是一种无状态调度。
 */
public class RpcLoadBalanceRoundRobin extends RpcLoadBalance {
    private AtomicInteger roundRobin = new AtomicInteger(0);

    @Override
    public RpcProtocol route(String serviceKey, Map<RpcProtocol, RpcClientHandler> connectedServerNodes) throws Exception {
        Map<String, List<RpcProtocol>> serviceMap = getServiceMap(connectedServerNodes);
        List<RpcProtocol> rpcProtocols = serviceMap.get(serviceKey);
        if (CollectionUtils.isNotEmpty(rpcProtocols)) {
            int size = rpcProtocols.size();
            int index = (roundRobin.getAndAdd(1) + size) % size;
            return rpcProtocols.get(index);
        }else {
            throw new Exception("Can not find connection for service: " + serviceKey);
        }
    }
}
