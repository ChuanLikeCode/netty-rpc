package com.sf.rpc.client.route;

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
import com.sf.rpc.common.protocol.RpcProtocol;
import com.sf.rpc.common.protocol.RpcServiceInfo;
import com.sf.rpc.config.NettyRpcConfig;
import com.sf.rpc.untils.ServiceUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author  : 01397429 周川
 * @version : 调度算法
 */
public abstract class RpcLoadBalance {

    public static NettyRpcConfig nettyRpcConfig;

    /**
     * 获取service的Map key为interface+Name value为zookeeper的注册服务信息
     *
     * @param connectedServerNode 连接信息
     * @return Map
     */
    protected Map<String, List<RpcProtocol>> getServiceMap(Map<RpcProtocol, RpcClientHandler> connectedServerNode) {
        Map<String, List<RpcProtocol>> serviceMap = new HashMap<>();
        if (MapUtils.isNotEmpty(connectedServerNode)) {
            for (RpcProtocol rpcProtocol : connectedServerNode.keySet()) {
                for (RpcServiceInfo serviceInfo : rpcProtocol.getServiceInfoList()) {
                    String serviceKey = ServiceUtils.makeServiceKey(nettyRpcConfig.getClientClassPath(), serviceInfo.getServiceName(), serviceInfo.getVersion());
                    List<RpcProtocol> orDefault = serviceMap.getOrDefault(serviceKey, new ArrayList<>());
                    orDefault.add(rpcProtocol);
                    serviceMap.putIfAbsent(serviceKey, orDefault);
                }
            }
        }
        return serviceMap;
    }

    /**
     * 调度路由
     *
     * @param serviceKey           key
     * @param connectedServerNodes 服务连接信息
     * @return RpcProtocol
     * @throws Exception e
     */
    public abstract RpcProtocol route(String serviceKey, Map<RpcProtocol, RpcClientHandler> connectedServerNodes) throws Exception;
}
