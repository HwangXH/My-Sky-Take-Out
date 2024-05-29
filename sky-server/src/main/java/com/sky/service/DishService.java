package com.sky.service;

import com.sky.dto.DishDTO;

public interface DishService {

    /**
     * 新增菜品和对应的口味（因为设计到菜品表和口味表）
     * @param dishDTO
     */
    public void saveWithFlavor(DishDTO dishDTO);
}
