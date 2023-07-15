package top.wuhunyu.mybatis.plus.cache.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户
 *
 * @author wuhunyu
 * @version 1.0
 * @date 2023-07-15 17:25
 */

@TableName("user")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    private static final long serialVersionUID = -2716722547404828511L;

    /**
     * 用户id
     */
    @TableId(type = IdType.ASSIGN_ID)
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
