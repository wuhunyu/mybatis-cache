package top.wuhunyu.mybatis.cache;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import top.wuhunyu.mybatis.plus.cache.domain.User;
import top.wuhunyu.mybatis.cache.mapper.UserMapper;

import java.io.IOException;
import java.io.InputStream;

/**
 * mybatis 二级缓存测试
 *
 * @author wuhunyu
 * @version 1.0
 * @date 2023-07-15 17:45
 */

@Slf4j
public class MybatisCacheTest {

    private SqlSessionFactory sqlSessionFactory;

    /**
     * 初始化 SqlSessionFactory
     */
    @Before
    public void initEnv() {
        try (InputStream inputStream = MybatisCacheTest.class
                .getClassLoader()
                .getResourceAsStream("mybatis-config.xml");

        ) {
            sqlSessionFactory = new SqlSessionFactoryBuilder()
                    .build(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testFirstCache1() {
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
    }

    @Test
    public void testFirstCache2() {
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
    }

    @Test
    public void testFirstCache3() {
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
    }

    @Test
    public void testFirstCache4() {
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
            sqlSession.rollback();
            Assert.assertNotSame(user1, user2);
        }
    }

    @Test
    public void testFirstCache5() {
        try (
                SqlSession sqlSession = sqlSessionFactory.openSession(true);
        ) {
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            User user1 = userMapper.findUserById(1L);
            log.info("user1: {}", user1);
            User user2 = userMapper.findUserById(1L);
            log.info("user2: {}", user2);
            Assert.assertSame(user1, user2);
        }
    }

    @Test
    public void testFirstCache6() {
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
    }

}
