package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品相关接口")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品: {}", dishDTO);
        //新增菜品后，菜品默认为停售，不会影响用户的redis读取效果，在起售后统一清理缓存
        //String key = "dish_" + dishDTO.getId();
        //clearCache(key);
        dishService.saveWithFlavor(dishDTO);
        return Result.success();
    }

    //因为是GET的Query格式，因此不用加@RequestBody，是在网址中以？格式查询
    //涉及到菜品表和分类表
    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO) {
        log.info("菜品分页查询: {}", dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    //@RequestParam利用MVC框架将前端给到的ids字符串解析成List
    @DeleteMapping
    @ApiOperation("菜品(批量)删除")
    public Result delete(@RequestParam List<Long> ids){
        log.info("菜品批量删除: {}", ids);
        //清空所有redis缓存，直接粗暴
        // TODO 是否有更好的方法提高redis操作效率？不过商家一般经常修改菜品的信息
        //clearCache("dish_*");
        dishService.deleteBatch(ids);
        return Result.success();
    }

    //用VO的原因是可以对应到接口所需要的属性
    //为后面修改菜品做铺垫
    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishVO> getById(@PathVariable Long id) {
        log.info("根据id查询菜品: {}", id);
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    @PutMapping
    @ApiOperation("修改菜品信息")
    @CacheEvict(cacheNames = "dishCache", key = "#dishDTO.categoryId")
    public Result update(@RequestBody DishDTO dishDTO) {
        log.info("修改菜品: {}", dishDTO);
        //clearCache("dish_*");
        dishService.updateWithFlavor(dishDTO);
        return Result.success();
    }

    @PostMapping("/status/{status}")
    @ApiOperation("起售或者停售菜品")
    @Caching(evict = {
            @CacheEvict(cacheNames = "dishCache", allEntries = true),
            @CacheEvict(cacheNames = "setmealCache", allEntries = true)
            })
    public Result enableOrDisable(@PathVariable Integer status, Long id){
        log.info("起售或者停售菜品: {}, {}", id, status);
        //clearCache("dish_*");
        dishService.enableOrDisable(status, id);
        return Result.success();
    }

    //在套餐涉及的菜品选择上，根据菜品的分类来呈现同一分类的菜品，根据菜品分类id查询菜品为后面做铺垫
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<Dish>> list(Long categoryId) {
        log.info("管理端根据分类id查询菜品: {}", categoryId);
        List<Dish> dishList = dishService.list(categoryId);
        return Result.success(dishList);
    }

    private void clearCache(String pattern){
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }
}
