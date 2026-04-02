# RPC Execution Framework

一个轻量级 RPC 框架，基于 **Nacos + Netty + Kryo** 构建。

## 🏗️ 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                     Consumer (客户端)                        │
│  ┌──────────┐  ┌──────────┐  ┌────────────┐  ┌──────────┐  │
│  │ 业务代码  │→│ 动态代理  │→│  负载均衡   │→│ Netty客户端│ │
│  └──────────┘  └──────────┘  └────────────┘  └──────────┘  │
└───────────────────────────┬─────────────────────────────────┘
                            │ TCP (自定义协议 + Kryo序列化)
                            │
┌───────────────────────────┼─────────────────────────────────┐
│                    Nacos 注册中心                             │
│                   (服务注册 & 发现)                           │
└───────────────────────────┼─────────────────────────────────┘
                            │
┌───────────────────────────┼─────────────────────────────────┐
│                     Provider (服务端)                        │
│  ┌──────────┐  ┌──────────┐  ┌────────────┐  ┌──────────┐  │
│  │ 服务实现  │←│ 请求分发  │←│ Kryo反序列化 │←│ Netty服务端│ │
│  └──────────┘  └──────────┘  └────────────┘  └──────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## 📦 模块结构

| 模块 | 说明 |
|------|------|
| `rpc-common` | 公共模型、枚举、异常定义 |
| `rpc-core` | 核心框架：序列化、注册中心、网络传输、代理 |
| `rpc-api` | 示例服务接口定义 |
| `rpc-server` | 示例服务端应用 |
| `rpc-client` | 示例客户端应用 |

## 🔧 技术选型

| 组件 | 技术 | 说明 |
|------|------|------|
| 注册中心 | **Nacos 2.3.2** | 服务注册与发现，支持健康检查 |
| 网络通信 | **Netty 4.1.108** | 高性能NIO异步通信框架 |
| 序列化 | **Kryo 5.6.0** | 高效的Java序列化库 |
| 负载均衡 | 自研 | 支持随机、轮询策略 |
| 异步调用 | **CompletableFuture** | 异步RPC调用支持 |

## 🔌 自定义协议

```
+--------+--------+--------+--------+-----------------+
| Magic  | Version| MsgType| Status |    RequestId    |
| 2bytes | 1byte  | 1byte  | 1byte  |     8bytes      |
+--------+--------+--------+--------+-----------------+
|   DataLength    |     Data (Kryo serialized)         |
|     4bytes      |          variable                  |
+-----------------+------------------------------------+
```

- **Magic (0xCAFE)**: 快速识别RPC协议包
- **Version**: 协议版本号，便于升级
- **MsgType**: 消息类型 (请求/响应/心跳)
- **DataLength**: 数据长度，解决TCP粘包/拆包问题

## 🚀 快速开始

### 前置条件

1. JDK 1.8+
2. Maven 3.6+
3. Nacos Server 2.x（[下载地址](https://github.com/alibaba/nacos/releases)）

### 1. 启动 Nacos

```bash
# 单机模式启动 Nacos
sh startup.sh -m standalone
# Windows
startup.cmd -m standalone
```

### 2. 编译项目

```bash
mvn clean install -DskipTests
```

### 3. 启动服务端

```bash
# 方式1: IDE中运行 ServerApplication.main()
# 方式2: 使用命令行 (可选参数: host port nacosAddress)
java -cp rpc-server/target/classes;rpc-core/target/classes;rpc-common/target/classes;rpc-api/target/classes com.leo.rpc.server.ServerApplication
```

### 4. 启动客户端

```bash
# 方式1: IDE中运行 ClientApplication.main()
# 方式2: 使用命令行 (可选参数: nacosAddress)
java -cp rpc-client/target/classes;rpc-core/target/classes;rpc-common/target/classes;rpc-api/target/classes com.leo.rpc.client.ClientApplication
```

## 📖 使用示例

### 定义服务接口 (rpc-api)

```java
public interface HelloService {
    String hello(String name);
    User getUserById(Long id);
}
```

### 服务端实现 (rpc-server)

```java
@RpcService
public class HelloServiceImpl implements HelloService {
    @Override
    public String hello(String name) {
        return "Hello, " + name + "!";
    }

    @Override
    public User getUserById(Long id) {
        return User.builder().id(id).name("User" + id).build();
    }
}

// 启动服务端
NettyServer server = new NettyServer("127.0.0.1", 9090, "127.0.0.1:8848");
server.publishService(new HelloServiceImpl(), HelloService.class);
server.start();
```

### 客户端调用 (rpc-client)

```java
// 创建服务发现
NacosServiceDiscovery discovery = new NacosServiceDiscovery("127.0.0.1:8848", new RandomLoadBalancer());

// 创建客户端
NettyClient client = new NettyClient(discovery);

// 获取代理对象 (像本地方法一样调用远程服务)
RpcClientProxy proxy = new RpcClientProxy(client);
HelloService helloService = proxy.getProxy(HelloService.class);

// 直接调用
String result = helloService.hello("Leo");       // -> "Hello, Leo!"
User user = helloService.getUserById(1001L);       // -> User{id=1001, name="User1001"}
```

## ✨ 核心特性

- ✅ **自定义协议**: 基于魔数的协议识别，LengthField解决粘包/拆包
- ✅ **Nacos注册中心**: 服务自动注册与发现，支持健康检查
- ✅ **Kryo高效序列化**: ThreadLocal线程安全，高性能零拷贝
- ✅ **连接复用**: Channel缓存，避免重复建连
- ✅ **异步调用**: CompletableFuture异步RPC，支持超时控制
- ✅ **负载均衡**: 随机/轮询策略，可扩展
- ✅ **优雅关闭**: JVM ShutdownHook，Netty优雅停机
- ✅ **服务分版本/分组**: 支持同一接口多版本共存
- ✅ **自定义注解**: @RpcService / @RpcReference
- ✅ **空闲检测**: IdleStateHandler心跳机制

## 📁 详细目录结构

```
rpc-execution/
├── pom.xml
├── README.md
├── rpc-common/                          # 公共模块
│   └── src/main/java/com/leo/rpc/common/
│       ├── model/
│       │   ├── RpcRequest.java          # RPC请求
│       │   ├── RpcResponse.java         # RPC响应
│       │   └── ServiceInfo.java         # 服务元信息
│       ├── enums/
│       │   └── RpcResponseCode.java     # 状态码枚举
│       └── exception/
│           └── RpcException.java        # 自定义异常
├── rpc-core/                            # 核心框架
│   └── src/main/java/com/leo/rpc/core/
│       ├── serialize/
│       │   ├── Serializer.java          # 序列化接口
│       │   └── KryoSerializer.java      # Kryo实现
│       ├── registry/
│       │   ├── ServiceRegistry.java     # 注册接口
│       │   ├── ServiceDiscovery.java    # 发现接口
│       │   └── nacos/
│       │       ├── NacosServiceRegistry.java
│       │       └── NacosServiceDiscovery.java
│       ├── loadbalance/
│       │   ├── LoadBalancer.java        # 负载均衡接口
│       │   ├── RandomLoadBalancer.java  # 随机策略
│       │   └── RoundRobinLoadBalancer.java # 轮询策略
│       ├── transport/
│       │   ├── codec/
│       │   │   ├── RpcProtocolConstants.java  # 协议常量
│       │   │   ├── RpcEncoder.java            # 编码器
│       │   │   └── RpcDecoder.java            # 解码器
│       │   ├── server/
│       │   │   ├── NettyServer.java           # Netty服务端
│       │   │   └── NettyServerHandler.java    # 服务端处理器
│       │   └── client/
│       │       ├── NettyClient.java           # Netty客户端
│       │       ├── NettyClientHandler.java    # 客户端处理器
│       │       └── UnprocessedRequests.java   # 异步请求管理
│       ├── proxy/
│       │   └── RpcClientProxy.java      # JDK动态代理
│       ├── provider/
│       │   ├── ServiceProvider.java     # 本地服务管理接口
│       │   └── DefaultServiceProvider.java
│       ├── config/
│       │   └── RpcConfig.java           # 框架配置
│       └── annotation/
│           ├── RpcService.java          # 服务端注解
│           └── RpcReference.java        # 客户端注解
├── rpc-api/                             # 示例API
│   └── src/main/java/com/leo/rpc/api/
│       ├── HelloService.java
│       └── model/User.java
├── rpc-server/                          # 示例服务端
│   └── src/main/java/com/leo/rpc/server/
│       ├── ServerApplication.java
│       └── service/HelloServiceImpl.java
└── rpc-client/                          # 示例客户端
    └── src/main/java/com/leo/rpc/client/
        └── ClientApplication.java
```

## License

MIT License
