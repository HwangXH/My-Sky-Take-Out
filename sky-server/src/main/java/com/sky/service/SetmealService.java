package com.sky.service;

import com.sky.dto.SetmealDTO;

public interface SetmealService {

    /**
     * 新增套餐，涉及菜品多选，要修改套餐表 和 套餐菜品关系表
     * @param setmealDTO
     */
    void saveWithDish(SetmealDTO setmealDTO);
}
