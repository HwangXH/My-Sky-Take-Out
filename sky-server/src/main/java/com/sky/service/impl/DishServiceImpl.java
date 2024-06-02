package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增菜品和对应的口味（因为涉及到菜品表和口味表）
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

    //为什么菜品分页查询用了VO，员工分页查询没有用VO，而是employee
    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());

        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 菜品批量删除
     * @param ids
     */
    //@Transactional事务注解保持一致性
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //判断当前菜品是否能被删除
        // TODO 反复循环地执行一条条的查询语句效率不高，可以先用where id in ..找到符合的数据，再for循环判断
        //1.是否在售中？
        for (Long id : ids) {
            //优化点 <select count(0) from dish where id in (?,?,?) and status = 1; >
            Dish dish = dishMapper.getById(id);

            if(dish.getStatus() == StatusConstant.ENABLE) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        //2.菜品是否关联有套餐?
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if(setmealIds!=null && setmealIds.size()>0) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }


        /*for (Long id : ids) {
            //删除菜品表的对应的数据
            dishMapper.deleteById(id);

            //删除口味表中与该菜品关联的口味数据,因为菜品的dish_id和菜品表的主键id是唯一对应的，因此可以用id表示是dish_id用在口味表
            dishFlavorMapper.deleteBySetmealId(id);
        }*/

        //优化点 delete from dish where id in (1,2,3)
        dishMapper.deleteByIds(ids);
        //优化点 delete from dish_flavor where dish_id in (1,2,3)
        dishFlavorMapper.deleteByDishIds(ids);
    }

    /**
     * 根据id查询菜品和对应的口味
     * @param id
     * @return
     */
    public DishVO getByIdWithFlavor(Long id) {
        //根据id查询菜品数据
        Dish dish = dishMapper.getById(id);

        //根据菜品id查询口味数据
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);

        //封装到VO
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavors);

        return dishVO;
    }

    /**
     * 根据id修改菜品信息和口味信息
     * @param dishDTO
     */
    public void updateWithFlavor(DishDTO dishDTO) {
        //修改菜品表的基本信息，而不需要修改DTO里的flavor
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.update(dish);

        //删除口味表原有数据，插入新口味数据
        dishFlavorMapper.deleteByDishId(dishDTO.getId());

        List<DishFlavor> flavors = dishDTO.getFlavors();
        // TODO 原有的菜品可能没有口味，或者说前端不会给dishId，因此都得要自己去获取
        if(flavors!=null && flavors.size()>0) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId());
            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 根据菜品分类id查询多个菜品，在管理端使用，只根据分类返回一些菜品
     * @param categoryId
     * @return
     */
    public List<Dish> list(Long categoryId) {

        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();

        //去dish表中去找符合条件（指在售的，分类id=？）的菜品，返回一个list
        List<Dish> dishList = dishMapper.list(dish);
        return dishList;
    }

    /**
     * 根据菜品分类id查询多个菜品，并且在Service层中进一步根据菜品名称查询口味返回给用户端去显示（因为VO要求有口味list）
     * @param categoryId
     * @return
     */
    public List<DishVO> listWithFlavor(Long categoryId) {
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        //根据分类id查询到该分类的所有菜品
        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        //根据菜品id查询到该菜品的所有口味，并且封装到VO中
        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }
        return dishVOList;
    }

    /**
     * 起售或者停售菜品
     * @param status
     * @param id
     */
    public void enableOrDisable(Integer status, Long id) {
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.update(dish);
    }
}
