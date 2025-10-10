# Flash Seckill - 高性能秒杀系统

## 项目简介

Flash Seckill 是一个基于 Spring Boot 3.2.0 的高性能秒杀系统，采用现代化的技术栈构建，具备高并发、高可用、分布式等特点。系统实现了完整的秒杀业务流程，包括用户认证、商品管理、秒杀下单、库存扣减、订单处理等功能。

## 技术栈

### 后端技术
- **框架**: Spring Boot 3.2.0
- **安全**: Spring Security + JWT
- **ORM**: MyBatis-Plus 3.5.11
- **数据库**: MySQL 8.0
- **缓存**: Redis + Redisson
- **消息队列**: RabbitMQ
- **构建工具**: Maven
- **Java版本**: 17

### 核心特性
- redis + lua实现原子性库存扣减
- Redis缓存优化查询性能
- 消息队列异步处理订单
- JWT无状态认证
- canal监听binlog日志实现数据一致性

## 项目结构

```
flash_seckill/
├── src/main/java/com/flash_seckill/
│   ├── controller/          # 控制器层
│   ├── service/             # 业务逻辑层
│   ├── mapper/              # 数据访问层
│   ├── pojo/                # 数据传输对象
│   ├── utils/               # 工具类
│   ├── config/              # 配置类
│   └── exception/           # 异常处理
├── src/main/resources/
│   ├── application.yaml     # 应用配置
│   └── seckill.lua          # Redis Lua脚本
├── sql/
│   └── create_tables.sql    # 数据库表结构
└── pom.xml                  # Maven依赖配置
```

## 快速开始

### 环境要求
- JDK 17+
- MySQL 8.0+
- Redis 6.0+
- RabbitMQ 3.8+
- Maven 3.6+
- Docker (可选，用于容器化部署)

### 方式一：Docker部署（推荐）

#### 1. 创建Docker网络
```bash
docker network create seckill-net
```

#### 2. 部署MySQL
```bash
docker run -d \
  --name mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=123456 \
  -v /home/bjw297/docker_volumes/mysql/conf/my.cnf:/etc/mysql/conf.d/my.cnf \
  -v /home/bjw297/docker_volumes/mysql/data:/var/lib/mysql \
  --network seckill-net \
  mysql:8.0
```

#### 3. 部署RabbitMQ
```bash
docker run -d \
  --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  -v /home/bjw297/docker_volumes/rabbitmq:/var/lib/rabbitmq \
  -e RABBITMQ_DEFAULT_USER=rabbit \
  -e RABBITMQ_DEFAULT_PASS=123456 \
  --hostname rabbit \
  --network seckill-net \
  rabbitmq:3.13-management
```

#### 4. 部署Canal
```bash
docker run -d \
  --name canal \
  -p 11111:11111 \
  -v /home/bjw297/docker_volumes/canal/conf/canal.properties:/home/admin/canal-server/conf/canal.properties \
  -v /home/bjw297/docker_volumes/canal/conf/instance.properties:/home/admin/canal-server/conf/example/instance.properties \
  -v /home/bjw297/docker_volumes/canal/logs:/home/admin/canal-server/logs \
  --network seckill-net \
  canal/canal-server:v1.1.7
```

#### 5. 部署Redis
```bash
docker run -d \
  --name redis \
  -p 6379:6379 \
  -v /home/bjw297/docker_volumes/redis/conf/redis.conf:/etc/redis/redis.conf \
  -v /home/bjw297/docker_volumes/redis/data:/data \
  --network seckill-net \
  redis:7.2 \
  redis-server /etc/redis/redis.conf
```

#### 6. 初始化数据库
```bash
# 进入MySQL容器
docker exec -it mysql mysql -uroot -p123456

# 创建数据库
CREATE DATABASE seckill_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 退出MySQL，执行SQL脚本
docker exec -i mysql mysql -uroot -p123456 seckill_system < sql/create_tables.sql
```

#### 7. 配置应用
修改 `src/main/resources/application.yaml` 中的服务连接地址：

```yaml
spring:
  datasource:
    url: jdbc:mysql://mysql:3306/seckill_system  # 使用容器名
    username: root
    password: 123456
  
  data:
    redis:
      host: redis  # 使用容器名
      password: ''  # Redis无密码
  
  rabbitmq:
    host: rabbitmq  # 使用容器名
    username: rabbit
    password: 123456
```

#### 8. 构建并运行应用
```bash
# 构建应用
mvn clean package -DskipTests

# 运行应用
java -jar target/flash_seckill-0.0.1-SNAPSHOT.jar
```

### 方式二：本地部署

#### 1. 数据库配置

1. 创建数据库：
```sql
CREATE DATABASE seckill_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 执行SQL脚本：
```bash
mysql -u root -p seckill_system < sql/create_tables.sql
```

#### 2. 应用配置

修改 `src/main/resources/application.yaml` 中的配置：

```yaml
# 数据库配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/seckill_system
    username: your_username
    password: your_password

# Redis配置
  data:
    redis:
      host: localhost
      password: your_redis_password

# RabbitMQ配置
  rabbitmq:
    host: localhost
    username: your_rabbitmq_username
    password: your_rabbitmq_password
```

#### 3. 启动应用

1. 编译项目：
```bash
mvn clean compile
```

2. 运行应用：
```bash
mvn spring-boot:run
```

3. 访问地址：http://localhost:8081

## API接口文档

### 认证接口

#### 用户登录
- **URL**: `POST /api/auth/login`
- **参数**:
```json
{
  "username": "string",
  "password": "string"
}
```
- **响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "jwt_token",
    "username": "string"
  }
}
```

#### 用户注册
- **URL**: `POST /api/auth/register`
- **参数**: 同登录接口
- **响应**: 注册成功信息

### 商品接口

#### 获取商品列表
- **URL**: `GET /api/product/list`
- **认证**: 需要JWT令牌
- **响应**: 商品列表信息

#### 获取商品详情
- **URL**: `GET /api/product/{id}`
- **认证**: 需要JWT令牌
- **响应**: 商品详细信息

#### 获取商品排行榜
- **URL**: `GET /api/product/rank`
- **认证**: 需要JWT令牌
- **响应**: 热门商品排行榜

### 秒杀接口

#### 秒杀下单
- **URL**: `POST /api/seckill/{productId}`
- **认证**: 需要JWT令牌
- **响应**: 下单结果

#### 查询订单状态
- **URL**: `GET /api/order/{orderId}`
- **认证**: 需要JWT令牌
- **响应**: 订单状态信息

## 核心功能实现

### 1. redis + lua库存扣减
使用Redis Lua脚本实现原子性的库存扣减操作，防止超卖：

```lua
-- seckill.lua
if redis.call('exists', KEYS[1]) == 1 then
    local stock = tonumber(redis.call('get', KEYS[1]))
    if stock <= 0 then
        return -1
    end
    if stock > 0 then
        redis.call('decr', KEYS[1])
        return stock - 1
    end
end
return -1
```

### 2. 消息队列异步处理
使用RabbitMQ实现订单的异步处理，提高系统吞吐量：

- **订单创建队列**: 处理秒杀成功的订单
- **订单超时队列**: 处理未支付的超时订单

### 3. 缓存优化
- Redis缓存商品信息
- Redis有序集合实现商品排行榜

### 4. 安全认证
- JWT无状态认证
- Spring Security权限控制
- 密码加密存储

**注意**: 本系统为学习演示用途，生产环境使用前请进行充分测试和安全评估。