package com.flash_seckill.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
@TableName("user")
public class User {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;
    private String nickName;
    private String icon;

    private String role;

    // 数据库自动填充
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
