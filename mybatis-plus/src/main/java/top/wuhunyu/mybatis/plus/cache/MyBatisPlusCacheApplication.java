package top.wuhunyu.mybatis.plus.cache;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MyBatisPlus 二级缓存测试主启动类
 *
 * @author wuhunyu
 * @version 1.0
 * @date 2023-07-15 20:31
 */

@SpringBootApplication
@MapperScan(basePackages = {"top.wuhunyu.mybatis.plus.cache.mapper"})
public class MyBatisPlusCacheApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyBatisPlusCacheApplication.class, args);
    }

}
