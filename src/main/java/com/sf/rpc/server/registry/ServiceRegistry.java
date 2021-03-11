package com.sf.rpc.server.registry;

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

import com.sf.rpc.common.config.Constant;
import com.sf.rpc.common.protocol.RpcProtocol;
import com.sf.rpc.common.protocol.RpcServiceInfo;
import com.sf.rpc.common.zookeeper.CuratorClient;
import com.sf.rpc.untils.ServiceUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author :  周川
 * @version : 服务注册
 */
public class ServiceRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ServiceRegistry.class);
    private CuratorClient curatorClient;//连接zookeeper的客户端
    private List<String> pathList = new ArrayList<>();
    public ServiceRegistry(String registerAddress){
        this.curatorClient = new CuratorClient(registerAddress,5000);
    }
    public void registerService(String host, int port, Map<String ,Object> serviceMap){
        //接口服务信息
        List<RpcServiceInfo> serviceInfoList = new ArrayList<>();
        for (String key:serviceMap.keySet()){
            String[] serviceInfo = key.split(ServiceUtils.SERVICE_CONCAT_TOKEN);
            if (serviceInfo.length > 0){
                RpcServiceInfo rpcServiceInfo = new RpcServiceInfo();
                rpcServiceInfo.setServiceName(serviceInfo[0]);
                if (serviceInfo.length == 2) {
                    rpcServiceInfo.setVersion(serviceInfo[1]);
                } else {
                    rpcServiceInfo.setVersion("");
                }
                logger.info("Register new service: {} ", key);
                serviceInfoList.add(rpcServiceInfo);
            }else {
                logger.warn("Can not get service name and version: {} ", key);
            }
        }
        try {
            //发送接口信息到zookeeper
            RpcProtocol rpcProtocol = new RpcProtocol();
            rpcProtocol.setHost(host);
            rpcProtocol.setPort(port);
            rpcProtocol.setServiceInfoList(serviceInfoList);
            String serviceData = rpcProtocol.toJson();
            byte[] bytes = serviceData.getBytes();
            String path = Constant.ZK_DATA_PATH + "-" + rpcProtocol.hashCode();
            this.curatorClient.createPathData(path,bytes);
            //收集注册信息
            pathList.add(path);
            logger.info("Register {} new service, host: {}, port: {}", serviceInfoList.size(), host, port);
        }catch (Exception e){
            logger.error("Register service fail, exception: {}", e.getMessage());
        }

        this.curatorClient.addConnectionStateListener((curatorFramework, connectionState) -> {
            if (connectionState == ConnectionState.RECONNECTED){
                logger.info("Connection state: {}, register service after reconnected", connectionState);
                registerService(host,port,serviceMap);
            }
        });

    }

    public void unregisterService(){
        logger.info("Unregister all service");
        for (String path:pathList){
            try {
                this.curatorClient.deletePath(path);
            }catch (Exception e){
                logger.error("Delete service path error: " + e.getMessage());
            }
        }
        this.curatorClient.close();
    }

}
