package com.sf.rpc.common.codec;

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

import com.sf.rpc.common.serializer.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author :  周川
 * @version : 解码器
 */
public class RpcDecoder extends ByteToMessageDecoder {

    private static final Logger logger = LoggerFactory.getLogger(RpcDecoder.class);

    private Class<?> clazz;
    private Serializer serializer;

    public RpcDecoder(Class<?> clazz,Serializer serializer){
        this.clazz = clazz;
        this.serializer = serializer;
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        //可读数据如果少于4字节说明事不完整的包 要等待下次继续读取
        if (byteBuf.readableBytes() < 4){
            return;
        }
        //做一个读取的标记
        byteBuf.markReaderIndex();
        //获取读取的长度
        int length = byteBuf.readInt();
        //如果读取的数据长度没有到达设定的长度 那么说明事不完整的包
        //重置读取的指针位置并返回等待下次数据到来读取
        if (byteBuf.readableBytes() < length){
            byteBuf.resetReaderIndex();
            return;
        }
        //读取传输的数据
        byte[] data = new byte[length];
        byteBuf.readBytes(data);
        try {
            //反序列化为对象
            Object deserializer = serializer.deserializer(data, clazz);
            list.add(deserializer);
        }catch (Exception e){
            logger.error("Decode error: " + e.toString());
        }

    }
}

