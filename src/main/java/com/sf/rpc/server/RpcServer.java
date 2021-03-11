package com.sf.rpc.server;

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

import com.sf.rpc.common.annotation.NettyRpcService;
import com.sf.rpc.config.NettyRpcConfig;
import com.sf.rpc.server.core.NettyServer;
import com.sf.rpc.server.registry.ServiceRegistry;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author :  周川
 * @version :
 */
public class RpcServer extends NettyServer implements ApplicationContextAware, InitializingBean, DisposableBean {
    public RpcServer(String serverAddress, String serviceRegistry) {
        super(serverAddress, serviceRegistry);
    }

    /**
     * 如果存在服务端则开启
     * 不存在则不开启服务端
     * @throws Exception 异常
     */
    @Override
    public void destroy() throws Exception {
        super.stop();
    }

    /**
     * 在Bean加载完成之后开启服务
     * 如果存在服务端则开启
     * 不存在则不开启服务端
     * @throws Exception 异常
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        super.start();
    }

    /**
     * 初始化 服务信息
     * @param applicationContext 上下文信息
     * @throws BeansException 异常
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(NettyRpcService.class);
        if (MapUtils.isNotEmpty(beansWithAnnotation)){
            for (Object serviceBean:beansWithAnnotation.values()){
                NettyRpcService annotation = serviceBean.getClass().getAnnotation(NettyRpcService.class);
                String name = annotation.value().getName();
                String version = annotation.version();
                super.addService(name,version,serviceBean);
            }
        }
    }
}
