package com.sf.rpc.untils;

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

import com.esotericsoftware.kryo.NotNull;
import com.sf.rpc.config.NettyRpcConfig;
import com.sf.rpc.server.core.RpcServerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @Author: 01397429 周川
 * @Description:
 * @Date: create in 2021/2/26 14:00
 */
public class ServiceUtils {
    //    private static final Logger logger = LoggerFactory.getLogger(ServiceUtils.class);
    public static final String SERVICE_CONCAT_TOKEN = "#";

    /**
     * 构造缓存key值    interfaceName#version
     * 根据客户端的类路径来构造 Key
     * 可有多个客户端 分布在不同的服务器上
     * 所以需要设置每个需要连接的客户端类路径
     *
     * @param packageName 客户端类路径
     * @param interfaceName 接口名字
     * @param version       接口版本
     * @return key
     */
    public static String makeServiceKey(String packageName, String interfaceName, String version) {
        String[] split = interfaceName.split("\\.");
        String serviceKey = packageName + "." + split[split.length - 1];
        if (version != null && version.trim().length() > 0) {
            serviceKey += SERVICE_CONCAT_TOKEN.concat(version);
        }
        return serviceKey;
    }
}