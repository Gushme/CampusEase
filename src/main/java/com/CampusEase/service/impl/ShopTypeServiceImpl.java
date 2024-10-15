package com.CampusEase.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.CampusEase.dto.Result;
import com.CampusEase.entity.ShopType;
import com.CampusEase.mapper.ShopTypeMapper;
import com.CampusEase.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.CampusEase.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryList() {
        // 1. 从redis查询商铺类型缓存
        String typeJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_TYPE_KEY);
        // 2. 判断是否存在
        if (StrUtil.isNotBlank(typeJson)) {
            // 3. 存在，直接返回
            List<ShopType> shopTypes = JSONUtil.toList(typeJson, ShopType.class);
            return Result.ok(shopTypes);
        }

        // 4. 不存在，查询数据库
        List<ShopType> list = query().orderByAsc("sort").list();

        // 5. 不存在，返回错误
        if(CollectionUtil.isEmpty(list)) {
            return Result.fail("商铺分类信息不存在!");
        }

        // 6. 存在，先写入redis
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_TYPE_KEY, JSONUtil.toJsonStr(list));

        // 7. 返回
        return Result.ok(list);
    }
}
