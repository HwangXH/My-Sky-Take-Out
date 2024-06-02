package com.sky.mapper;

import com.sky.entity.SetmealDish;
import com.sky.vo.DishItemVO;
import org.apache.ibatis.annotations.Delete;
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

    /**
     * 删除一个 套餐 对应的套餐-菜品关系
     * @param setmealId
     */
    @Delete("delete from setmeal_dish where setmeal_id = #{setmealId}")
    void deleteBySetmealId(Long setmealId);

    /**
     * 批量删除套餐-菜品表中的对应(批量)套餐的数据
     * @param setmealIds
     */
    void deleteBySetmealIds(List<Long> setmealIds);

    /**
     * 根据id查询套餐和套餐内的菜品
     * @param setmealId
     * @return
     */
    @Select("select * from setmeal_dish where setmeal_id = #{setmealId}")
    List<SetmealDish> getBySetmealId(Long setmealId);

    /**
     * 根据套餐id去查询包含的菜品的某些字段构成一个套餐（但是是用户所理解的有什么菜品，有多少份菜品）
     * @param id
     * @return
     */
    @Select("select sd.name, sd.copies, d.image, d.description " +
            "from setmeal_dish sd left join dish d on sd.dish_id = d.id " +
            "where sd.setmeal_id = #{setmealId}")
    List<DishItemVO> getDishItemById(Long id);
}
