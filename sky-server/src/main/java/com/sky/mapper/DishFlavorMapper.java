package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DishFlavorMapper {

    /**
     * 批量插入口味数据
     * @param flavors
     */
    void insertBatch(List<DishFlavor> flavors);

    /**
     * 批量删除口味表中的对应某菜品的口味
     * @param dishId
     */
    @Delete("delete dish_flavor from dish_flavor where dish_id = #{dishId}")
    void deleteByDishId(Long dishId);

    /**
     * 批量删除口味表中的对应(批量)品的口味
     * @param dishIds
     */
    void deleteByDishIds(List<Long> dishIds);
}
