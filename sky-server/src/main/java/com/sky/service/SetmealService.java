package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetmealService {

    /**
     * 新增套餐，涉及菜品多选，要修改套餐表 和 套餐菜品关系表
     * @param setmealDTO
     */
    void saveWithDish(SetmealDTO setmealDTO);

    /**
     * 套餐分类查询
     * @param setmealPageQueryDTO
     * @return
     */
    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 批量删除套餐
     * @param ids
     */
    void deleteBatch(List<Long> ids);

    /**
     * 根据套餐id查询套餐和套餐内的菜品，包括套餐的状态，分类等管理端才需要的数据信息，但是不包括菜品的份数，返回的是一个套餐的VO
     * @param id
     * @return
     */
    SetmealVO getByIdWithDish(Long id);

    /**
     * 修改套餐
     * @param setmealDTO
     */
    void updateWithDish(SetmealDTO setmealDTO);

    /**
     * 起售或者停售套餐
     * @param status
     * @param id
     */
    void enableOrDisable(Integer status, Long id);

    /**
     * 根据套餐分类id查询多个套餐，用在客户端
     * @param categoryId
     * @return
     */
    List<Setmeal> list(Long categoryId);

    /**
     * 根据套餐id查询套餐内的菜品数据，包括菜品的份数，图片等用户关注的数据，返回的是一个由菜品itemVO组成的list
     * @param id
     * @return
     */
    List<DishItemVO> getDishItemById(Long id);
}
