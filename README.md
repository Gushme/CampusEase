# CampusEase

CampusEase是一个校园服务平台，提供优惠券秒杀、商铺浏览、社交互动等功能，旨在为校园用户提供便捷的服务和社交体验。

## 项目概述

本项目是基于Spring Boot的Java Web应用，采用了Redis作为缓存、RabbitMQ作为消息队列，实现了高并发场景下的秒杀功能、商铺信息展示、用户社交等功能。项目特别关注高并发场景下的性能优化，使用了Redis分布式锁、Lua脚本、消息队列等技术保证系统的稳定性和可靠性。

## 技术栈

- **后端框架**: Spring Boot 2.7.17
- **数据库**: MySQL 5.7
- **ORM框架**: MyBatis-Plus 3.4.3
- **缓存**: Redis, Caffeine (本地缓存)
- **消息队列**: RabbitMQ
- **分布式锁**: Redisson
- **工具库**: Hutool, Guava (限流), Lombok
- **AI集成**: LangChain4j (OpenAI集成)
- **Java版本**: Java 8

## 主要功能

1. **优惠券秒杀系统**
   - 基于Redis的高性能秒杀
   - Lua脚本保证原子性操作
   - 支持活动时间控制和库存管理

2. **商铺服务**
   - 商铺信息展示
   - 商铺分类管理
   - 商铺搜索功能

3. **用户社交功能**
   - 博客发布与评论
   - 用户关注关系
   - 内容分享功能

4. **用户管理**
   - 用户注册与登录
   - 用户信息管理
   - 授权认证

## 安装与运行

### 环境要求

- Java 8+
- MySQL 5.7+
- Redis 6.0+
- RabbitMQ 3.8+
- Maven 3.6+

### 安装步骤

1. 克隆代码仓库
   ```bash
   git clone [仓库地址]
   cd CampusEase
   ```

2. 配置数据库
   - 创建名为`CampusEase`的数据库
   - 修改`application.yaml`中的数据库连接信息

3. 配置Redis和RabbitMQ
   - 确保Redis和RabbitMQ服务已启动
   - 根据需要修改`application.yaml`中的Redis和RabbitMQ连接信息

4. 编译与运行
   ```bash
   mvn clean package
   java -jar target/CampusEase-0.0.1-SNAPSHOT.jar
   ```

5. 访问应用
   - 默认端口: 8081
   - 访问地址: `http://localhost:8081`

## 项目结构

```
src/main/java/com/CampusEase/
├── controller/         # 控制器层，处理HTTP请求
├── service/            # 业务逻辑层
├── mapper/             # MyBatis映射接口
├── entity/             # 数据实体类
├── dto/                # 数据传输对象
├── config/             # 配置类
├── utils/              # 工具类
├── intercepter/        # 拦截器
└── CampusEaseApplication.java  # 应用程序入口
```

## 性能优化

本项目针对高并发场景进行了多项优化：

1. **缓存优化**
   - Redis缓存热点数据
   - Caffeine本地缓存减轻Redis压力

2. **秒杀优化**
   - Lua脚本保证原子性
   - 消息队列异步处理订单
   - 分布式锁防止超卖

3. **限流保护**
   - Guava实现接口限流
   - AOP切面实现细粒度控制

## 性能测试

使用JMeter进行压力测试，项目支持200并发用户同时请求秒杀接口，系统稳定且无超卖现象。

## 贡献指南

欢迎贡献代码，提交问题和改进建议。请遵循以下步骤：

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交您的更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 打开一个 Pull Request

## 许可证

[待定] - 请添加适合您项目的许可证 