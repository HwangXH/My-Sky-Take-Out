package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 根据菜品id来查询相关套餐id，菜品id和套餐id呈现多对多的关系
     * @param dishIds
     * @return
     */
    //select setmeal_id from setmeal_dish where dish_id in (1,2,3,4) 动态SQL
    //<foreach collection="dishIds" item="dishId" separator="," open="(" close=")">
    //根据传入的参数dishIds进行动态sql，每次遍历得到的item为dishId，通过添加','和开头结尾补上()来形成sql
    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);

    /**
     * 批量保存套餐和菜品的关系数据
     * @param dishes
     */
    void insertBatch(List<SetmealDish> dishes);
}
