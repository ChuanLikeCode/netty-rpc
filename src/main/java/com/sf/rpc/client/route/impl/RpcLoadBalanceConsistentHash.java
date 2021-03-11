package com.sf.rpc.client.route.impl;

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

import com.google.common.hash.Hashing;
import com.sf.rpc.client.handler.RpcClientHandler;
import com.sf.rpc.client.route.RpcLoadBalance;
import com.sf.rpc.common.protocol.RpcProtocol;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * @author :  周川
 * @version : 一致性hash算法
 */
public class RpcLoadBalanceConsistentHash extends RpcLoadBalance {
    @Override
    public RpcProtocol route(String serviceKey, Map<RpcProtocol, RpcClientHandler> connectedServerNodes) throws Exception {
        Map<String, List<RpcProtocol>> serviceMap = getServiceMap(connectedServerNodes);
        List<RpcProtocol> addressList = serviceMap.get(serviceKey);
        if (CollectionUtils.isNotEmpty(addressList)) {
            return addressList.get(Hashing.consistentHash(serviceKey.hashCode(),addressList.size()));
        } else {
            throw new Exception("Can not find connection for service: " + serviceKey);
        }
    }
}

