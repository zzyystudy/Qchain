# qchian-api

统一的服务调用契约模块，按用户指定的名称使用 `qchian-api`。

```text
com.zxyy.api
├── client              ProductClient / OrderClient / UserClient
├── dto
│   ├── product
│   ├── order
│   └── user
├── vo
│   ├── product
│   ├── order
│   └── user
└── config              内部请求令牌
```

其他服务直接依赖本模块，不依赖被调用服务的实现模块或数据库实体：

```xml
<dependency>
    <groupId>com.zxyy</groupId>
    <artifactId>qchian-api</artifactId>
</dependency>
```

在调用方启动类注册需要的客户端：

```java
@EnableFeignClients(clients = ProductClient.class)
```

使用 Nacos 注册的 `product-service`、`order-service`、`user-service`。内部接口调用方还需要和被调用方配置相同的 `qchain.internal.token`。调用后台内部接口不透传浏览器 JWT。

开启 `spring.cloud.openfeign.okhttp.enabled=true`、关闭 `spring.cloud.openfeign.httpclient.hc5.enabled` 后，OpenFeign 自动创建和销毁 OkHttp 客户端及其连接池，无需 Java Bean 配置。通过 `spring.cloud.openfeign.httpclient.max-connections`、`time-to-live`、`time-to-live-unit` 配置连接池。当前版本的 `max-connections` 用于池的最大空闲连接数，同时配置 OkHttp Dispatcher；同步 Feign 调用的并发数不由 Dispatcher 限制。

客户端返回项目统一的 `Result<T>`。调用方必须检查 `code == 1`；订单服务的 `OrderProductGateway` 已统一检查业务失败和必要返回数据，失败会触发事务回滚或消费重试。

可选的直连地址：`qchain.clients.product-url`、`qchain.clients.order-url`、`qchain.clients.user-url`。为空时使用服务发现。

OpenFeign 的 OkHttp 和 LoadBalancer 配置依据：[Spring Cloud OpenFeign 官方文档](https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/)。
