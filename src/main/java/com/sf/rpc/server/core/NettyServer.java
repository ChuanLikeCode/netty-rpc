package com.sf.rpc.server.core;

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

import com.sf.rpc.config.NettyRpcConfig;
import com.sf.rpc.server.registry.ServiceRegistry;
import com.sf.rpc.untils.ServiceUtils;
import com.sf.rpc.untils.ThreadPoolUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @Author: 01397429 周川
 * @Description:
 * @Date: create in 2021/2/26 13:53
 */
public class NettyServer extends Server{

    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    private Thread thread;//由一个线程来后台启动 Netty-RPC 服务
    private String serverAddress;//服务器 IP 地址
    private ServiceRegistry serviceRegistry;//zookeeper注册信息
    private Map<String ,Object> serviceMap = new HashMap<>();//记录服务信息,用来反馈客户端调用的方法结果
    public static NettyRpcConfig nettyRpcConfig;

    public NettyServer(String serverAddress, String serviceRegistry) {
        this.serverAddress = serverAddress;
        this.serviceRegistry = new ServiceRegistry(serviceRegistry);
    }

    /**
     * 注册 server 类调用
     * @param interfaceName 完整类名
     * @param version 接口版本
     * @param serviceBean 需要调用的类
     */
    public void addService(String interfaceName,String version,Object serviceBean){
        String[] clientClassPath = nettyRpcConfig.getClientClassPath().split(",");
        for (String packageName : clientClassPath) {
            logger.info("Adding service, interface: {}, version: {}, bean：{}", interfaceName, version, serviceBean);
            serviceMap.put(ServiceUtils.makeServiceKey(packageName,interfaceName,version),serviceBean);
        }
    }

    @Override
    public void start() throws Exception {
        thread = new Thread(new Runnable() {
            ThreadPoolExecutor threadPoolExecutor = ThreadPoolUtil.makeServerThreadPool(NettyServer.class.getName(),
                    16,32);
            @Override
            public void run() {
                EventLoopGroup bossGroup = new NioEventLoopGroup();
                EventLoopGroup workerGroup = new NioEventLoopGroup();
                try {
                    ServerBootstrap serverBootstrap = new ServerBootstrap()
                            .group(bossGroup,workerGroup)
                            .channel(NioServerSocketChannel.class)
                            .childHandler(new RpcServerInitializer(serviceMap,threadPoolExecutor))
                            .option(ChannelOption.SO_BACKLOG,128)
                            .childOption(ChannelOption.SO_KEEPALIVE,true);
                    String[] split = serverAddress.split(":");
                    int port = Integer.parseInt(split[1]);
                    ChannelFuture sync = serverBootstrap.bind(split[0], port).sync();
                    if (serviceRegistry != null){
                        serviceRegistry.registerService(split[0],port,serviceMap);
                    }
                    logger.info("Netty-Rpc Server started on port {}", port);
                    sync.channel().closeFuture().sync();
                }catch (Exception e){
                    if (e instanceof InterruptedException) {
                        logger.info("Rpc server remoting server stop");
                    } else {
                        logger.error("Rpc server remoting server error", e);
                    }
                }finally {
                    try {
                        assert serviceRegistry != null;
                        serviceRegistry.unregisterService();
                        workerGroup.shutdownGracefully();
                        bossGroup.shutdownGracefully();
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                }
            }
        });
        thread.start();
    }

    @Override
    public void stop() throws Exception {
        // destroy server thread
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }
}

