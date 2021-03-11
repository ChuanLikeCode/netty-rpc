package com.sf.rpc.client.connect;


//                            _ooOoo_
//                           o8888888o
//                           88" . "88
//                           (| -_- |)
//                           O\  =  /O
//                        ____/`---'\____
//                      .'  \\|     |//  `.
//                     /  \\|||  :  |||//  \
//                    /  _||||| -:- |||||-  \
//                    |   | \\\  -  /// |   |
//                    | \_|  ''\---/''  |   |
//                    \  .-\__  `-`  ___/-. /
//                  ___`. .'  /--.--\  `. . __
//               ."" '<  `.___\_<|>_/___.'  >'"".
//              | | :  `- \`.;`\ _ /`;.`/ - ` : | |
//              \  \ `-.   \_ __\ /__ _/   .-` /  /
//         ======`-.____`-.___\_____/___.-`____.-'======
//                            `=---='
//        ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
//                      佛祖保佑         永无BUG

import com.sf.rpc.client.handler.RpcClientHandler;
import com.sf.rpc.client.handler.RpcClientInitializer;
import com.sf.rpc.client.route.RpcLoadBalance;
import com.sf.rpc.client.route.impl.RpcLoadBalanceConsistentHash;
import com.sf.rpc.client.route.impl.RpcLoadBalanceLFU;
import com.sf.rpc.client.route.impl.RpcLoadBalanceLRU;
import com.sf.rpc.client.route.impl.RpcLoadBalanceRoundRobin;
import com.sf.rpc.common.protocol.RpcProtocol;
import com.sf.rpc.common.protocol.RpcServiceInfo;
import com.sf.rpc.config.NettyRpcConfig;
import com.sf.rpc.untils.ServiceUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author : 周川
 * @version : 服务连接管理器
 */
public class ConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);
    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(4, 8,
            600L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1000));

    private Map<RpcProtocol, RpcClientHandler> connectedServerNodes = new ConcurrentHashMap<>();
    private CopyOnWriteArraySet<RpcProtocol> rpcProtocolSet = new CopyOnWriteArraySet<>();
    private ReentrantLock lock = new ReentrantLock();
    private Condition connected = lock.newCondition();
    private long waitTimeout = 5000;
    private RpcLoadBalance loadBalance;
    private volatile boolean isRunning = true;
    private NettyRpcConfig nettyRpcConfig;

    public void setNettyRpcConfig(NettyRpcConfig nettyRpcConfig) {
        this.nettyRpcConfig = nettyRpcConfig;
    }

    private ConnectionManager() {

    }

    public void setLoadBalance(String balance) {
        switch (balance) {
            case "RoundRobin":
                this.loadBalance = new RpcLoadBalanceRoundRobin();
                break;
            case "LRU":
                this.loadBalance = new RpcLoadBalanceLRU();
                break;
            case "LFU":
                this.loadBalance = new RpcLoadBalanceLFU();
                break;
            case "ConsistentHash":
                this.loadBalance = new RpcLoadBalanceConsistentHash();
                break;
        }
    }

    private static class SingletonHolder {
        private static final ConnectionManager instance = new ConnectionManager();
    }

    public static ConnectionManager getInstance() {
        return SingletonHolder.instance;
    }

    public void updateConnectedServer(List<RpcProtocol> serviceList) {
        // Now using 2 collections to manage the service info and TCP connections because making the connection is async
        // Once service info is updated on ZK, will trigger this function
        // Actually client should only care about the service it is using
        if (serviceList != null && serviceList.size() > 0) {
            // Update local server nodes cache
            HashSet<RpcProtocol> serviceSet = new HashSet<>(serviceList.size());
            serviceSet.addAll(serviceList);

            // Add new server info
            for (final RpcProtocol rpcProtocol : serviceSet) {
                if (!rpcProtocolSet.contains(rpcProtocol)) {
                    connectServerNode(rpcProtocol);
                }
            }

            // Close and remove invalid server nodes
            for (RpcProtocol rpcProtocol : rpcProtocolSet) {
                if (!serviceSet.contains(rpcProtocol)) {
                    logger.info("Remove invalid service: " + rpcProtocol.toJson());
                    RpcClientHandler handler = connectedServerNodes.get(rpcProtocol);
                    if (handler != null) {
                        handler.close();
                    }
                    connectedServerNodes.remove(rpcProtocol);
                    rpcProtocolSet.remove(rpcProtocol);
                }
            }
        } else {
            // No available service
            logger.error("No available service!");
            for (RpcProtocol rpcProtocol : rpcProtocolSet) {
                RpcClientHandler handler = connectedServerNodes.get(rpcProtocol);
                if (handler != null) {
                    handler.close();
                }
                connectedServerNodes.remove(rpcProtocol);
                rpcProtocolSet.remove(rpcProtocol);
            }
        }
    }

    private void connectServerNode(RpcProtocol rpcProtocol) {
        if (rpcProtocol.getServiceInfoList() == null || rpcProtocol.getServiceInfoList().isEmpty()) {
            logger.info("No service on node, host: {}, port: {}", rpcProtocol.getHost(), rpcProtocol.getPort());
            return;
        }
        rpcProtocolSet.add(rpcProtocol);
        logger.info("New service node, host: {}, port: {}", rpcProtocol.getHost(), rpcProtocol.getPort());
        for (RpcServiceInfo serviceProtocol : rpcProtocol.getServiceInfoList()) {
            logger.info("New service info, name: {}, version: {}", serviceProtocol.getServiceName(), serviceProtocol.getVersion());
        }
        final InetSocketAddress remotePeer = new InetSocketAddress(rpcProtocol.getHost(), rpcProtocol.getPort());
        threadPoolExecutor.submit(() -> {
            Bootstrap b = new Bootstrap();
            b.group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new RpcClientInitializer());

            ChannelFuture channelFuture = b.connect(remotePeer);
            channelFuture.addListener((ChannelFutureListener) channelFuture1 -> {
                if (channelFuture1.isSuccess()) {
                    logger.info("Successfully connect to remote server, remote peer = " + remotePeer);
                    RpcClientHandler handler = channelFuture1.channel().pipeline().get(RpcClientHandler.class);
                    connectedServerNodes.put(rpcProtocol, handler);
                    handler.setRpcProtocol(rpcProtocol);
                    signalAvailableHandler();
                } else {
                    logger.error("Can not connect to remote server, remote peer = " + remotePeer);
                }
            });
        });
    }

    private void signalAvailableHandler() {
        lock.lock();
        try {
            connected.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private boolean waitingForHandler() throws InterruptedException {
        lock.lock();
        try {
            logger.warn("Waiting for available service");
            return connected.await(this.waitTimeout, TimeUnit.MILLISECONDS);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 根据 serviceKey 选择服务端连接
     *
     * @param className 接口名字
     * @param version 类版本
     * @return RpcClientHandler
     * @throws Exception 异常
     */
    public RpcClientHandler chooseHandler(String className, String version) throws Exception {
        int size = connectedServerNodes.values().size();
        while (isRunning && size <= 0) {
            try {
                waitingForHandler();
                size = connectedServerNodes.values().size();
            } catch (InterruptedException e) {
                logger.error("Waiting for available service is interrupted!", e);
            }
        }
        //如果有多个客户端，则获取对应客户端访问的类名key 来获取服务的的 handler
        String serviceKey = ServiceUtils.makeServiceKey(nettyRpcConfig.getClientClassPath(), className, version);
        RpcProtocol rpcProtocol = loadBalance.route(serviceKey, connectedServerNodes);
        RpcClientHandler handler = connectedServerNodes.get(rpcProtocol);
        if (handler != null) {
            return handler;
        } else {
            throw new Exception("Can not get available connection");
        }
    }

    public void removeHandler(RpcProtocol rpcProtocol) {
        rpcProtocolSet.remove(rpcProtocol);
        connectedServerNodes.remove(rpcProtocol);
        logger.info("Remove one connection, host: {}, port: {}", rpcProtocol.getHost(), rpcProtocol.getPort());
    }

    public void stop() {
        isRunning = false;
        for (RpcProtocol rpcProtocol : rpcProtocolSet) {
            RpcClientHandler handler = connectedServerNodes.get(rpcProtocol);
            if (handler != null) {
                handler.close();
            }
            connectedServerNodes.remove(rpcProtocol);
            rpcProtocolSet.remove(rpcProtocol);
        }
        signalAvailableHandler();
        threadPoolExecutor.shutdown();
        eventLoopGroup.shutdownGracefully();
    }
}
