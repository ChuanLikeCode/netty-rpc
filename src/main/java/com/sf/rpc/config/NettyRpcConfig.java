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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * @author : 01397429 周川
 * @version :
 */
@EnableConfigurationProperties(NettyRpcConfig.class)
@ConfigurationProperties(prefix = "netty.rpc")
public class NettyRpcConfig {
    private String registerAddress;
    private String balance = "RoundRobin";
    private String clientClassPath;
    private int serverRpcPort = 9700;
    private boolean client = false;//作为客户端开启
    private boolean server = false;//作为服务的开启

    public boolean isServer() {
        return server;
    }

    public void setServer(boolean server) {
        this.server = server;
    }

    public boolean isClient() {
        return client;
    }

    public void setClient(boolean client) {
        this.client = client;
    }

    public int getServerRpcPort() {
        return serverRpcPort;
    }

    public void setServerRpcPort(int serverRpcPort) {
        this.serverRpcPort = serverRpcPort;
    }

    public String getRegisterAddress() {
        return registerAddress;
    }

    public void setRegisterAddress(String registerAddress) {
        this.registerAddress = registerAddress;
    }

    public String getBalance() {
        return balance;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }

    public String getClientClassPath() {
        return clientClassPath;
    }

    public void setClientClassPath(String clientClassPath) {
        this.clientClassPath = clientClassPath;
    }
}
