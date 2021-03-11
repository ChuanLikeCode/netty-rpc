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

import com.sf.rpc.common.codec.Beat;
import com.sf.rpc.common.codec.RpcRequest;
import com.sf.rpc.common.codec.RpcResponse;
import com.sf.rpc.config.NettyRpcConfig;
import com.sf.rpc.untils.ServiceUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.sf.cglib.reflect.FastClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author :  周川
 * @version :
 */
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {
    private static final Logger logger = LoggerFactory.getLogger(RpcServerHandler.class);
    private final Map<String, Object> handlerMap;
    private final ThreadPoolExecutor serverHandlerPool;
    public static NettyRpcConfig nettyRpcConfig;

    public RpcServerHandler(Map<String, Object> handlerMap, ThreadPoolExecutor serverHandlerPool) {
        this.handlerMap = handlerMap;
        this.serverHandlerPool = serverHandlerPool;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcRequest rpcRequest) throws Exception {
        //心跳检测数据
        if (Beat.BEAT_ID.equalsIgnoreCase(rpcRequest.getRequestId())) {
            logger.info("Server read heartbeat ping");
            return;
        }
        //线程池执行返回结果
        serverHandlerPool.execute(() -> {
            logger.info("Receive request " + rpcRequest.getRequestId());
            RpcResponse rpcResponse = new RpcResponse();
            rpcResponse.setRequestId(rpcRequest.getRequestId());
            try {
                Object handle = handle(rpcRequest);
                rpcResponse.setResult(handle);
            } catch (InvocationTargetException e) {
                rpcResponse.setError(e.getMessage());
                logger.error("RPC Server handle request error ", e);
            }
            channelHandlerContext.writeAndFlush(rpcResponse).addListener(chanelFuture -> {
                logger.info("Send response for request " + rpcRequest.getRequestId());
            });
        });
    }


    private Object handle(RpcRequest rpcRequest) throws InvocationTargetException {
        String className = rpcRequest.getClassName();
        String version = rpcRequest.getVersion();
        Object serviceBean = null;
        String[] clientClassPath = nettyRpcConfig.getClientClassPath().split(",");
        for (String packageName : clientClassPath) {
            serviceBean = handlerMap.get(ServiceUtils.makeServiceKey(packageName, className, version));
            if (serviceBean != null) {
                break;
            }
        }
        if (serviceBean == null) {
            logger.error("Can not find service implement with interface name: {} and version: {}", className, version);
            return null;
        }
        Class<?> aClass = serviceBean.getClass();
        String methodName = rpcRequest.getMethodName();
        Class<?>[] parameterTypes = rpcRequest.getParameterTypes();
        Object[] parameters = rpcRequest.getParameters();
        //cglib
        FastClass fastClass = FastClass.create(aClass);
        //根据方法描述符找到方法索引
        int index = fastClass.getIndex(methodName, parameterTypes);
        return fastClass.invoke(index, serviceBean, parameters);
    }
}
