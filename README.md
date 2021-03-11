# Netty-RPC工具
一款基于Netty的RPC工具，使用这款工具可以在网络之间发送更少的信息，来获得同样的结果。
## 开启zookeeper
例如:运行三个zookeeper节点：192.168.56.22:2181,192.168.56.23:2181,192.168.56.24:2181
## Spring 项目配置如下
无需显示启动，简单配置就能使用
```
netty:
  rpc:
    register-address: 192.168.56.22:2181,192.168.56.23:2181,192.168.56.24:2181 #zookeeper的地址
    client-class-path: com.test.rpcclient.service #客户端存放service类的包路径 多个客户端用英文逗号分割
    server-rpc-port: 9700 #服务端开启的TCP端口号
    server: true #是否作为客户端开启 则必须要有NettyService注解
    client: true #是否作为服务端开启
    balance: RoundRobin #负载均衡策略 RoundRobin(轮询法)、LRU(Least Recently Used 即最近最少使用)、LFU(Least frequently used 最不经常使用)、ConsistentHash(一致性Hash算法)
```
### NettyRpcService 注解
```
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface NettyRpcService {
    Class<?> value();//类

    String version() default "";//接口实现的版本
}
```

### 服务端配置
```
public interface PersonService {
    String getPersonName();
}

@NettyRpcService(value = PersonService.class,version = "1.0")
public class PersonServiceImpl implements PersonService {
    @Override
    public String getPersonName() {
        return "zhouchuan";
    }
}
```

### 客户端调用
```
@Autowired
private RpcClient rpcClient;

PersonService service = rpcClient.createService(PersonService.class,"1.0");
String name = service.getPersonName();
```

## 其他项目使用
### 服务端配置
```
public interface PersonService {
    String getPersonName();
}

@NettyRpcService(value = PersonService.class,version = "1.0")
public class PersonServiceImpl implements PersonService {
    @Override
    public String getPersonName() {
        return "zhouchuan";
    }
}

RpcServer rpcServer = new RpcServer("127.0.0.1:8080","192.168.56.22:2181,192.168.56.23:2181,192.168.56.24:2181");
PersonService personService = new PersonService();
rpcServer.addService(PersonService.getClass().getName(),"1.0",personService)
rpcServer.start();
```

### 客户端调用
```
PersonService service = rpcClient.createService(PersonService.class,"1.0");
String name = service.getPersonName();
```

