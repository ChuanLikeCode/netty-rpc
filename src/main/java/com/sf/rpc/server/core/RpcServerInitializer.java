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

import com.sf.rpc.common.codec.*;
import com.sf.rpc.common.serializer.KryoSerializer;
import com.sf.rpc.common.serializer.Serializer;
import com.sf.rpc.config.NettyRpcConfig;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author :  周川
 * @version :
 */
public class RpcServerInitializer extends ChannelInitializer<SocketChannel> {
    private Map<String, Object> handlerMap;
    private ThreadPoolExecutor threadPoolExecutor;


    public RpcServerInitializer(Map<String, Object> handlerMap, ThreadPoolExecutor threadPoolExecutor) {

        this.handlerMap = handlerMap;
        this.threadPoolExecutor = threadPoolExecutor;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        Serializer serializer = KryoSerializer.class.newInstance();
        socketChannel.pipeline()
                .addLast(new IdleStateHandler(0,0, Beat.BEAT_TIMEOUT, TimeUnit.SECONDS))
                .addLast(new LengthFieldBasedFrameDecoder(65535,0,4,0,0))
                .addLast(new RpcDecoder(RpcRequest.class,serializer))
                .addLast(new RpcEncoder(RpcResponse.class,serializer))
                .addLast(new RpcServerHandler(handlerMap, threadPoolExecutor));
    }
}

