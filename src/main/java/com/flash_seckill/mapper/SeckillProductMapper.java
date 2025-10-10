package com.flash_seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flash_seckill.pojo.entity.SeckillProduct;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;

public interface SeckillProductMapper extends BaseMapper<SeckillProduct> {

    @Select("select id from seckill_product")
    Collection<Long> idList();
}
