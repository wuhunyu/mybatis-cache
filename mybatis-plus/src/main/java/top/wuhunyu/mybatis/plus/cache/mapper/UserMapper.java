package top.wuhunyu.mybatis.plus.cache.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.CacheNamespaceRef;
import org.apache.ibatis.annotations.Param;
import top.wuhunyu.mybatis.plus.cache.domain.User;

/**
 * 用户 mapper
 *
 * @author wuhunyu
 * @version 1.0
 * @date 2023-07-15 17:27
 */

//@CacheNamespaceRef(UserMapper.class)
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
