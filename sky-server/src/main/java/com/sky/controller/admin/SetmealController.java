package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/setmeal")
@Api(tags = "套餐相关接口")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    // TODO 套餐内的菜品没能够设置口味
    @PostMapping
    @ApiOperation("新增套餐")
    public Result save(@RequestBody SetmealDTO setmealDTO){
        log.info("新增套餐: {}", setmealDTO);
        setmealService.saveWithDish(setmealDTO);
        return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation("套餐分类查询")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO){
        log.info("套餐分页查询，参数为:{}", setmealPageQueryDTO);
        PageResult pageResult = setmealService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    @DeleteMapping
    @ApiOperation("套餐(批量)删除")
    public Result delete(@RequestParam List<Long> ids){
        log.info("套餐批量删除: {}", ids);
        setmealService.deleteBatch(ids);
        return Result.success();
    }

    //为后续的修改套餐做铺垫
    @GetMapping("/{id}")
    @ApiOperation("根据id查询套餐")
    public Result<SetmealVO> getById(@PathVariable Long id){
        log.info("根据id查询套餐");
        SetmealVO setmealVO = setmealService.getByIdWithDish(id);
        return Result.success(setmealVO);
    }

    @PutMapping
    @ApiOperation("修改套餐")
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    public Result update(@RequestBody SetmealDTO setmealDTO){
        log.info("修改套餐: {}", setmealDTO);
        setmealService.updateWithDish(setmealDTO);
        return Result.success();
    }

    //因为新增套餐和删除套餐都要判断套餐当前是否停售，停售才可操作，且起售用户才会看到
    //因此只需要在起售或停售的接口中清理缓存
    // TODO 进一步细化key的取值，来区分两类套餐的清理，因为套餐起售停售只操作1个，可只清理对应分类的缓存（但是效率提升可能并不明显）
    // TODO 优化bug，停售菜品时，套餐也要停售
    @PostMapping("/status/{status}")
    @ApiOperation("起售或者停售套餐")
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    public Result enableOrDisable(@PathVariable Integer status, Long id){
        log.info("起售或者停售套餐: {}, {}", id, status);
        setmealService.enableOrDisable(status, id);
        return Result.success();
    }
}
