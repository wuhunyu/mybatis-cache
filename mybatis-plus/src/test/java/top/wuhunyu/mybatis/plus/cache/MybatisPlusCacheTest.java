package top.wuhunyu.mybatis.plus.cache;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.wuhunyu.mybatis.plus.cache.domain.User;
import top.wuhunyu.mybatis.plus.cache.mapper.UserMapper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * MyBatisPlus 二级缓存测试
 *
 * @author wuhunyu
 * @version 1.0
 * @date 2023-07-15 20:30
 */

@SpringBootTest(classes = MyBatisPlusCacheApplication.class)
@Slf4j
public class MybatisPlusCacheTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    public void testMybatisPlusCache1() {
        User user1 = userMapper.findUserById(1L);
        log.info("user1: {}", user1);
        User user2 = userMapper.findUserById(1L);
        log.info("user2: {}", user2);
    }

    @Test
    public void testMybatisPlusCache2() {
        User user1 = userMapper.selectById(1L);
        log.info("user1: {}", user1);
        User user2 = userMapper.selectById(1L);
        log.info("user2: {}", user2);
    }

    @Test
    public void testMybatisPlusCache3() throws InterruptedException {
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
    }

    @Test
    public void testMybatisPlusCache4() throws InterruptedException {
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
    }

}
