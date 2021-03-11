package com.sf.rpc.config;

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

import com.sf.rpc.client.ObjectProxy;
import com.sf.rpc.client.RpcClient;
import com.sf.rpc.client.connect.ConnectionManager;
import com.sf.rpc.client.route.RpcLoadBalance;
import com.sf.rpc.server.RpcServer;
import com.sf.rpc.server.core.NettyServer;
import com.sf.rpc.server.core.RpcServerHandler;
import com.sf.rpc.untils.ServiceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.*;
import java.util.Enumeration;

/**
 * @Author: 01397429 周川
 * @Description:
 * @Date: create in 2021/3/9 19:20
 */
@Configuration
@EnableConfigurationProperties(NettyRpcConfig.class)
@ConditionalOnProperty(prefix = "netty.rpc", value = "enabled", matchIfMissing = true)
public class NettyRpcAutoConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(NettyRpcAutoConfiguration.class);
    @Autowired
    private NettyRpcConfig nettyRpcConfig;

    @Bean
    @ConditionalOnProperty(prefix = "netty.rpc", name = "client", havingValue = "true")
    public RpcClient rpcClient() {
        RpcLoadBalance.nettyRpcConfig = nettyRpcConfig;
        ObjectProxy.nettyRpcConfig = nettyRpcConfig;
        ConnectionManager.getInstance().setLoadBalance(nettyRpcConfig.getBalance());
        ConnectionManager.getInstance().setNettyRpcConfig(nettyRpcConfig);
        return new RpcClient(nettyRpcConfig.getRegisterAddress());
    }

    @Bean
    @ConditionalOnProperty(prefix = "netty.rpc", name = "server", havingValue = "true")
    public RpcServer rpcServer() throws Exception {
        NettyServer.nettyRpcConfig = nettyRpcConfig;
        RpcServerHandler.nettyRpcConfig = nettyRpcConfig;
        String hostAddress = getLocalHostAddress().getHostAddress();
        return new RpcServer(hostAddress + ":" + nettyRpcConfig.getServerRpcPort(), nettyRpcConfig.getRegisterAddress());
    }

    private InetAddress getLocalHostAddress() throws UnknownHostException {
        Enumeration allNetInterfaces;
        try {
            allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress ip = null;
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();

                Enumeration addresses = netInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    ip = (InetAddress) addresses.nextElement();
                    if (!ip.isSiteLocalAddress() && !ip.isLoopbackAddress() && !ip.getHostAddress().contains(":")) {
                        if (ip instanceof Inet4Address) {
                            return ip;
                        }
                    }
                }
            }
        } catch (SocketException e) {
            logger.error("Unable to get server IP address");
        }

        InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
        if (jdkSuppliedAddress == null) {
            throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
        }
        return jdkSuppliedAddress;
    }


}