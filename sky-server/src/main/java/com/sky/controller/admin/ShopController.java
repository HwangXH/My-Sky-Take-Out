package com.sky.controller.admin;

import com.sky.constant.StatusConstant;
import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

// 为了避免和user的ShopController冲突，因为类名相同，bean名其实都是shopController，因此要修改bean名
@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Api(tags = "店铺相关接口")
@Slf4j
public class ShopController {

    // TODO 能否和user内的常量同时合为一体？都在常量类中设置
    public static final String Key = "shop_status";

    @Autowired
    private RedisTemplate redisTemplate;

    //并没有用3层结构去实现，而是就在Control层就硬实现了，原因可能在于3层架构更适合MySQL且复杂的情况
    @PutMapping("/{status}")
    @ApiOperation("设置店铺营业状态")
    public Result setStatus(@PathVariable Integer status){
        log.info("设置店铺营业状态为: {}", status.equals(StatusConstant.ENABLE) ? "营业" : "闭店");
        redisTemplate.opsForValue().set(Key, status);
        return Result.success();
    }

    @GetMapping("/status")
    @ApiOperation("管理端获取营业状态")
    public Result<Integer> getStatus(){
        Integer status = (Integer) redisTemplate.opsForValue().get(Key);
        log.info("管理端获取到店铺营业状态为: {}",status.equals(StatusConstant.ENABLE) ? "营业" : "闭店");
        return Result.success(status);
    }

}
