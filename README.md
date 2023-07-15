### MyBatis 缓存

MyBatis 有两级缓存，其中一级缓存默认开启，二级缓存需要手动开启。不过 MyBatis 的缓存很容易失效，所以建议保持默认配置只开启一级缓存即可。本文将以案例的形式来介绍 MyBatis 的缓存

> 本文所使用的例子完整示例存放在 [GitHub](https://github.com/wuhunyu/mybatis-cache.git) 中

#### MyBatis 的缓存分为 一级缓存 和 二级缓存

#### 一级缓存

一级缓存 默认开启，同一个 SqlSession 对象共享一个缓存

##### 失效场景

- 没有使用同一个 SqlSession
- 发生了任意修改类操作
- SqlSession commit 或者手动调用了 SqlSession 的 clearCache() 方法
- localCacheScope 属性配置为 STATEMENT

#### 二级缓存

二级缓存需要手动开启，同一个 namespace 共享一个缓存，也可以使用配置的方法使得多个 namespace 共享一个缓存

失效场景

- 发生了任意修改类操作，会清空所有的二级缓存。当然，一级缓存也会被清空
- 如果使用 MyBatisPlus 默认提供的查询语句，没有在 Mapper 接口上配置 @CacheNamespaceRef 也会导致二级缓存不生效
- SqlSession commit 或者手动调用了 SqlSession 的 clearCache() 方法
- mapper 映射文件没有配置 cache 标签

### 一级缓存示例

项目工程

<img src="https://wuhunyu-images.oss-cn-heyuan.aliyuncs.com/images/image-20230715194540549.png" alt="image-20230715194540549" style="zoom:67%;" />

MyBatis 核心配置

```xml
<settings>
    <!-- 二级缓存关闭 -->
    <setting name="cacheEnabled" value="false"/>
    <!-- 一级缓存配置为 session 级别，简单理解就是开启一级缓存 -->
    <setting name="localCacheScope" value="SESSION"/>
    <!-- 开启下划线转驼峰 -->
    <setting name="mapUnderscoreToCamelCase" value="true"/>
    <!-- 日志实现 -->
    <setting name="logImpl" value="SLF4J"/>
</settings>
```

logback 日志配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration scan="false">
    <contextName>mybatis</contextName>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <Pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%logger{50}] - %msg%n</Pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <logger name="org.springframework" level="WARN" additivity="false">
        <appender-ref ref="CONSOLE"/>
    </logger>

    <logger name="org.mybatis" level="DEBUG" additivity="false">
        <appender-ref ref="CONSOLE"/>
    </logger>

    <!-- mapper 层开启 debug 日志 -->
    <logger name="top.wuhunyu.mybatis.cache.mapper" level="DEBUG" additivity="false">
        <appender-ref ref="CONSOLE"/>
    </logger>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

User 实体

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    private static final long serialVersionUID = -2716722547404828511L;

    /**
     * 用户id
     */
    private Long id;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 年龄
     */
    private Integer age;

}
```

UserMapper

```java
public interface UserMapper {

    /**
     * 根据用户id查询用户信息
     *
     * @param id 用户id 非空
     * @return 用户实体对象
     */
    User findUserById(@Param("id") Long id);

    /**
     * 根据用户id修改用户信息
     *
     * @param user 用户实体对象
     */
    void updateUserById(@Param("user") User user);

}
```

UserMapper.xml 映射文件

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="top.wuhunyu.mybatis.cache.mapper.UserMapper">

    <select id="findUserById" resultType="top.wuhunyu.mybatis.cache.domain.User">
        select
            u.id,
            u.user_name,
            u.age
        from
            `user` u
        where
            u.id = #{id}
    </select>

    <update id="updateUserById">
        update
            `user`
        set
            user_name = #{user.userName},
            age = #{user.age}
        where
            id = #{user.id}
    </update>

</mapper>
```

#### 一级缓存生效演示

##### 同一个 SqlSession 同一个 Mapper 对象分别查询同一条 sql 时

```java
try (
    // 关闭自动提交事务
    SqlSession sqlSession = sqlSessionFactory.openSession(false);
) {
    UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
    User user1 = userMapper.findUserById(1L);
    log.info("user1: {}", user1);
    User user2 = userMapper.findUserById(1L);
    log.info("user2: {}", user2);
    sqlSession.commit();
    Assert.assertSame(user1, user2);
}
```

日志打印情况如下

![image-20230715195757074](https://wuhunyu-images.oss-cn-heyuan.aliyuncs.com/images/image-20230715195757074.png)

图中只打印了一次 sql，表示第二次查询并没有真的发起 sql 查询，而是从一级缓存中直接获取。由于一级缓存可以理解为一个 Map，并不存在序列化操作，因此直接比较 user1 和 user2 的内存地址也是相同的

---

##### 同一个 SqlSession 不同 Mapper 对象分别查询同一条 sql 时

```java
try (
    // 关闭自动提交事务
    SqlSession sqlSession = sqlSessionFactory.openSession(false);
) {
    UserMapper userMapper1 = sqlSession.getMapper(UserMapper.class);
    UserMapper userMapper2 = sqlSession.getMapper(UserMapper.class);
    User user1 = userMapper1.findUserById(1L);
    log.info("user1: {}", user1);
    User user2 = userMapper2.findUserById(1L);
    log.info("user2: {}", user2);
    sqlSession.commit();
    Assert.assertSame(user1, user2);
}
```

日志打印情况如下

![image-20230715200233467](https://wuhunyu-images.oss-cn-heyuan.aliyuncs.com/images/image-20230715200233467.png)

图中只打印了一次 sql，表示一级缓存生效

#### 一级缓存失效演示

##### 没有使用同一个 SqlSession

```java
try (
    // 关闭自动提交事务
    SqlSession sqlSession1 = sqlSessionFactory.openSession(false);
    SqlSession sqlSession2 = sqlSessionFactory.openSession(false);
) {
    UserMapper userMapper1 = sqlSession1.getMapper(UserMapper.class);
    UserMapper userMapper2 = sqlSession2.getMapper(UserMapper.class);
    User user1 = userMapper1.findUserById(1L);
    log.info("user1: {}", user1);
    sqlSession1.commit();
    User user2 = userMapper2.findUserById(1L);
    log.info("user2: {}", user2);
    sqlSession2.commit();
    Assert.assertNotSame(user1, user2);
}
```

日志打印情况如下

![image-20230715200606833](https://wuhunyu-images.oss-cn-heyuan.aliyuncs.com/images/image-20230715200606833.png)

图中分别打印了两次 sql，user1 和 user2 的内存地址也不一样了，表示一级缓存失效了

##### 发生了任意修改类操作

```java
try (
    // 关闭自动提交事务
    SqlSession sqlSession = sqlSessionFactory.openSession(false);
) {
    UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
    User user1 = userMapper.findUserById(1L);
    log.info("user1: {}", user1);
    userMapper.updateUserById(new User(2L, "李四-2", 21));
    User user2 = userMapper.findUserById(1L);
    log.info("user2: {}", user2);
    sqlSession.commit();
    Assert.assertNotSame(user1, user2);
}
```

日志打印情况如下

![image-20230715200510948](https://wuhunyu-images.oss-cn-heyuan.aliyuncs.com/images/image-20230715200510948.png)

同样打印了两次查询 sql，这是由于 updateUserById 发生了修改操作导致的缓存失效

##### SqlSession commit 或者手动调用了 SqlSession 的 clearCache() 方法

```java
try (
    // 关闭自动提交事务
    SqlSession sqlSession = sqlSessionFactory.openSession(false);
) {
    UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
    User user1 = userMapper.findUserById(1L);
    log.info("user1: {}", user1);
    sqlSession.clearCache();
    User user2 = userMapper.findUserById(1L);
    log.info("user2: {}", user2);
    sqlSession.commit();
    Assert.assertNotSame(user1, user2);
}
```

日志打印情况如下

![image-20230715201054331](https://wuhunyu-images.oss-cn-heyuan.aliyuncs.com/images/image-20230715201054331.png)

由于手动触发了 clearCache，导致一级缓存失效了

##### localCacheScope 属性配置为 STATEMENT

修改 localCacheScope 的值为 STATEMENT 

拿之前一级缓存生效的例子

```java
try (
    // 关闭自动提交事务
    SqlSession sqlSession = sqlSessionFactory.openSession(false);
) {
    UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
    User user1 = userMapper.findUserById(1L);
    log.info("user1: {}", user1);
    User user2 = userMapper.findUserById(1L);
    log.info("user2: {}", user2);
    sqlSession.commit();
    Assert.assertNotSame(user1, user2);
}
```

日志打印情况如下

![image-20230715201422829](https://wuhunyu-images.oss-cn-heyuan.aliyuncs.com/images/image-20230715201422829.png)

localCacheScope 为 STATEMENT 时，表示缓存作用于 STATEMENT，而一个 STATEMENT 就是一条 sql，也就表示一级缓存失效了

### 二级缓存示例

项目工程

<img src="https://wuhunyu-images.oss-cn-heyuan.aliyuncs.com/images/image-20230715205656283.png" alt="image-20230715205656283" style="zoom:67%;" />

application.yml 配置

```yaml
spring:
  application:
    name: mybatis-plus
  datasource:
    druid:
      driver-class-name: com.mysql.cj.jdbc.Driver
      url: jdbc:mysql://127.0.0.1:3306/test
      username: root
      password: 123456

server:
  port: 10000

# mybatis-plus 配置
mybatis-plus:
  configuration:
    # 开启下划线转驼峰
    map-underscore-to-camel-case: true
    # 日志实现
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
    # 二级缓存开启
    cache-enabled: true
    # 一级缓存配置为 session 级别，简单理解就是开启一级缓存
    local-cache-scope: session
  # mapper.xml 位置
  mapper-locations: classpath*:/top/wuhunyu.mybatis/plus/cache/mapper/**/*Mapper.xml
```

主启动类配置 mapper 扫描路径

![image-20230715205819902](https://wuhunyu-images.oss-cn-heyuan.aliyuncs.com/images/image-20230715205819902.png)

切换成了 MybatisPlus 之后，先看看一级缓存是否还生效

#### 一级缓存生效演示

##### 同一个 SqlSession，使用 Mybatis 原生 sql 写法

```java
User user1 = userMapper.findUserById(1L);
log.info("user1: {}", user1);
User user2 = userMapper.findUserById(1L);
log.info("user2: {}", user2);
```

日志打印情况如下

![image-20230715210250844](https://wuhunyu-images.oss-cn-heyuan.aliyuncs.com/images/image-20230715210250844.png)

可以看到，查询的 sql 语句只被执行了一次，日志还打印了缓存被击中的概率为 0.5，说明第二次查询确实从缓存中获取成功了

另外，日志有一条 WARN。这是由于二级缓存的结果会序列化保存的缘故

通过 debug 可以观察到两个 user 对象的 hash 值并不相同，表示不是同一个对象，但确实走了二级缓存，这是由于二级缓存反序列导致的

![image-20230715210637063](https://wuhunyu-images.oss-cn-heyuan.aliyuncs.com/images/image-20230715210637063.png)

这条 WARN 日志就是针对序列化可能产生 bug 而打印的警告，详情可以通过日志给出的[链接](https://docs.oracle.com/pls/topic/lookup?ctx=javase15&id=GUID-8296D8E8-2B93-4B9A-856E-0A65AF9B8C66)自行了解

此处以及之后的例子都不对这个 WARN 日志进行处理

##### 同一个 SqlSession，使用 MybatisPlus api 的写法

在 UserMapper 上加上 `@CacheNamespaceRef(UserMapper.class)` 注解

```java
@CacheNamespaceRef(UserMapper.class)
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户id查询用户信息
     *
     * @param id 用户id 非空
     * @return 用户实体对象
     */
    User findUserById(@Param("id") Long id);

    /**
     * 根据用户id修改用户信息
     *
     * @param user 用户实体对象
     */
    void updateUserById(@Param("user") User user);

}
```

测试用例如下

```java
User user1 = userMapper.selectById(1L);
log.info("user1: {}", user1);
User user2 = userMapper.selectById(1L);
log.info("user2: {}", user2);
```

日志打印情况如下

![image-20230715211058952](https://wuhunyu-images.oss-cn-heyuan.aliyuncs.com/images/image-20230715211058952.png)

能够观察到走了二级缓存

##### 不同 SqlSession，使用 MybatisPlus api 的写法

```java
User user1 = userMapper.selectById(1L);
log.info("user1: {}", user1);
ExecutorService executor = Executors.newSingleThreadExecutor();
CountDownLatch countDownLatch = new CountDownLatch(1);
executor.execute(() -> {
    User user2 = userMapper.selectById(1L);
    log.info("user2: {}", user2);
    countDownLatch.countDown();
});
countDownLatch.await();
```

日志打印情况如下

![image-20230715211604068](https://wuhunyu-images.oss-cn-heyuan.aliyuncs.com/images/image-20230715211604068.png)

也是没有问题的

#### 二级缓存失效演示

##### 发生了任意修改类操作

```java
User user1 = userMapper.selectById(1L);
log.info("user1: {}", user1);
userMapper.updateUserById(new User(2L, "张三", 21));
ExecutorService executor = Executors.newSingleThreadExecutor();
CountDownLatch countDownLatch = new CountDownLatch(1);
AtomicReference<User> userAtomicReference = new AtomicReference<>();
executor.execute(() -> {
    User user2 = userMapper.selectById(1L);
    log.info("user2: {}", user2);
    userAtomicReference.set(user2);
    countDownLatch.countDown();
});
countDownLatch.await();
```

日志打印情况如下

![image-20230715211821349](https://wuhunyu-images.oss-cn-heyuan.aliyuncs.com/images/image-20230715211821349.png)

可以看到二级缓存的集中率都是 0.0，表示二级缓存没有被击中

##### 如果使用 MyBatisPlus 默认提供的查询语句，没有在 Mapper 接口上配置 @CacheNamespaceRef

```java
User user1 = userMapper.selectById(1L);
log.info("user1: {}", user1);
User user2 = userMapper.selectById(1L);
log.info("user2: {}", user2);
```

日志打印情况如下

![image-20230715212124114](https://wuhunyu-images.oss-cn-heyuan.aliyuncs.com/images/image-20230715212124114.png)

发生了两次 sql 查询，说明缓存失效

##### mapper 映射文件没有配置 cache 标签

```java
User user1 = userMapper.findUserById(1L);
log.info("user1: {}", user1);
User user2 = userMapper.findUserById(1L);
log.info("user2: {}", user2);
```

日志打印情况如下

![image-20230715212450671](https://wuhunyu-images.oss-cn-heyuan.aliyuncs.com/images/image-20230715212450671.png)