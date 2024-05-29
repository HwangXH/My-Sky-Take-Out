package com.sky.service.impl;

import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    /**
     * 新增菜品和对应的口味（因为设计到菜品表和口味表）
     * @param dishDTO
     */
    //因为要修改两张表，因此引入事务去处理，保证方法是原子性
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {

        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        //菜品表插入1条数据
        dishMapper.insert(dish);

        //dish的xml中加入useGeneratedKeys="true"表示需要主键值，keyProperty="id"来对应Dish对象的id属性
        //获取insert语句生成的主键值
        Long dishId = dish.getId();

        //口味表插入多条数据
        //口味表的insert需求dish_id，但是菜品还没添加完成，无法获得通过get获得dishDTO中的，要从dish的xml中获得
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors!=null && flavors.size()>0) {
            //一个菜品有多个口味，每个口味都是相同菜品的id
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }
}
