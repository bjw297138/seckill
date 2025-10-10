package com.flash_seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flash_seckill.pojo.entity.User;
import org.apache.ibatis.annotations.Select;


public interface UserMapper extends BaseMapper<User> {

    @Select("select * from user where phone = #{phone}")
    User queryByPhone(String phone);

    @Select("select * from user where username = #{username}")
    User selectByUsername(String username);
}
