package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "用户端菜品浏览接口")
public class DishController {

    @Autowired
    private DishService dishService;

    //redis操作无需进入service层
    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    @Cacheable(cacheNames = "dishCache", key = "#categoryId")
    public Result<List<DishVO>> list(Long categoryId) {
        log.info("客户端根据分类id查询菜品: {}", categoryId);

        //方法一 自己控制缓存使用
        /*//构造redis使用的key
        String key = "dish_" + categoryId;
        //查询redis中是否有当前菜品的缓存数据
        List<DishVO> list = (List<DishVO>) redisTemplate.opsForValue().get(key);
        if(list!=null && list.size()>0) {
            //如果存在缓存则直接返回
            return Result.success(list);
        }

        //不存在，查询数据库，并且将查询到的数据存入缓存
        // TODO 是否该设置一下数据的缓存时间，定期清理掉一些缓存保证空间不爆炸
        list = dishService.listWithFlavor(categoryId);
        redisTemplate.opsForValue().set(key, list);*/

        //方法二 用注解使用缓存
        List<DishVO> list = dishService.listWithFlavor(categoryId);
        return Result.success(list);
    }

}
