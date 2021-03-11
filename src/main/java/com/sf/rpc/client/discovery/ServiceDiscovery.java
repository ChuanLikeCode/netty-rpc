package com.sf.rpc.client.discovery;

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

import com.sf.rpc.client.connect.ConnectionManager;
import com.sf.rpc.common.config.Constant;
import com.sf.rpc.common.protocol.RpcProtocol;
import com.sf.rpc.common.zookeeper.CuratorClient;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author :  周川
 * @version : 发现注册的服务信息
 */
public class ServiceDiscovery {
    private static final Logger logger = LoggerFactory.getLogger(ServiceDiscovery.class);
    private CuratorClient curatorClient;

    public ServiceDiscovery(String registryAddress) {
        this.curatorClient = new CuratorClient(registryAddress);
        discoveryService();
    }

    private void discoveryService() {
        try {
            logger.info("Get initial service info");
            //获取服务信息
            getServiceAndUpdateServer();
            //监听服务变更信息
            curatorClient.watchPathChildrenNode(Constant.ZK_REGISTRY_PATH, (curatorFramework, pathChildrenCacheEvent) -> {
                PathChildrenCacheEvent.Type type = pathChildrenCacheEvent.getType();
                switch (type) {
                    case CONNECTION_RECONNECTED:
                        logger.info("Reconnected to zk, try to get latest service list");
                        getServiceAndUpdateServer();
                        break;
                    case CHILD_ADDED:
                    case CHILD_UPDATED:
                    case CHILD_REMOVED:
                        logger.info("Service info changed, try to get latest service list");
                        getServiceAndUpdateServer();
                        break;
                }
            });
        } catch (Exception e) {
            logger.error("Watch node exception: " + e.getMessage());
        }
    }

    /**
     * 获取服务信息并更新
     */
    private void getServiceAndUpdateServer() {
        try {
            List<String> nodeList = curatorClient.getChildren(Constant.ZK_REGISTRY_PATH);
            List<RpcProtocol> dataList = new ArrayList<>();
            for (String node : nodeList) {
                logger.info("Service node: " + node);
                byte[] data = curatorClient.getData(Constant.ZK_REGISTRY_PATH + "/" + node);
                RpcProtocol rpcProtocol = RpcProtocol.fromJson(new String(data));
                dataList.add(rpcProtocol);
            }
            logger.info("Service node data: {}", dataList);
            UpdateConnectedServer(dataList);
        } catch (Exception e) {
            logger.error("Get node exception: " + e.getMessage());
        }
    }

    private void UpdateConnectedServer(List<RpcProtocol> dataList) {
        ConnectionManager.getInstance().updateConnectedServer(dataList);
    }

    public void stop() {
        this.curatorClient.close();
    }
}
