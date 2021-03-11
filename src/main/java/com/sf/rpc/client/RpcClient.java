package com.sf.rpc.client;

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
import com.sf.rpc.client.discovery.ServiceDiscovery;
import com.sf.rpc.common.annotation.RpcAutowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author :  周川
 * @version :
 */
public class RpcClient implements ApplicationContextAware, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(RpcClient.class);

    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16, 16,
            600L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1000));
    private ServiceDiscovery serviceDiscovery;
    public RpcClient(String address) {
        this.serviceDiscovery = new ServiceDiscovery(address);
    }

    public <T> T createService(Class<T> interfaceClass, String version) {
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new ObjectProxy<T>(interfaceClass,version));
    }

    public static void submit(Runnable task) {
        threadPoolExecutor.submit(task);
    }

    @Override
    public void destroy() throws Exception {
        this.stop();
    }

    public void stop() {
        threadPoolExecutor.shutdown();
        serviceDiscovery.stop();
        ConnectionManager.getInstance().stop();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            Field[] fields = bean.getClass().getDeclaredFields();
            try {
                for (Field field : fields) {
                    RpcAutowired rpcAutowired = field.getAnnotation(RpcAutowired.class);
                    if (rpcAutowired != null) {
                        String version = rpcAutowired.version();
                        field.setAccessible(true);
                        field.set(bean, createService(field.getType(), version));
                    }
                }
            } catch (IllegalAccessException e) {
                logger.error(e.toString());
            }
        }
    }
}
