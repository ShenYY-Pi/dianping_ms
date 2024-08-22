package com.dp.controller;


import com.dp.dto.Result;
import com.dp.entity.ShopType;
import com.dp.service.IShopTypeService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 前端控制器
 * </p>
 */
@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    @GetMapping("list")
    public Result queryTypeList() {
        String cacheKey = "shopTypeList";
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();

        // 尝试从Redis缓存中获取数据
        List<ShopType> typeList = (List<ShopType>) ops.get(cacheKey);

        if (typeList == null) {
            // 如果缓存中没有数据，则从数据库中查询
            typeList = typeService.query().orderByAsc("sort").list();

            // 将查询结果缓存到Redis，并设置过期时间（例如30分钟）
            ops.set(cacheKey, typeList, 30, TimeUnit.MINUTES);
        }

        // 返回结果
        return Result.ok(typeList);
    }
}
