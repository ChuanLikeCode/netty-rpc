package com.sf.rpc.common.serializer;

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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoPool;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;

/**
 * @Author: 01397429 周川
 * @Description: 主要用来序列化和反序列化得方法
 * @Date: create in 2021/2/25 18:00
 */
public class KryoSerializer extends Serializer {

    private KryoPool pool = KryoPoolFactory.getInstance();

    /**
     * 序列化对象
     *
     * @param obj 对象
     * @param <T> 模板
     * @return 字节数据
     */
    @Override
    public <T> byte[] serializer(T obj) {
        // get kryoPool
        Kryo kryo = pool.borrow();
        //write stream
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);
        try {
            kryo.writeObject(output, obj);
            output.close();
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                byteArrayOutputStream.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            pool.release(kryo);
        }
    }

    /**
     * 反序列化对象
     * @param bytes 字节数据
     * @param clazz 对象类
     * @param <T> 模板
     * @return Object
     */
    @Override
    public <T> Object deserializer(byte[] bytes, Class<T> clazz) {
        Kryo kryo = pool.borrow();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        Input input = new Input(byteArrayInputStream);
        try {
            Object result = kryo.readObject(input,clazz);
            input.close();
            return result;
        }catch (Exception e){
            throw new RuntimeException(e);
        }finally {
            try {
                byteArrayInputStream.close();
            }catch (Exception ex){
                throw new RuntimeException(ex);
            }
            pool.release(kryo);
        }
    }
}
