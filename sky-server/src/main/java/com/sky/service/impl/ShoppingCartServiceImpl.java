package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 新增购物车商品
     * @param shoppingCartDTO
     */
    public void add(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        //userId从jwt拦截器的中threadlocal获取
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);
        //虽然是list，但是实际上list长度为1，因为根据用户、菜品或者套餐名、口味只能查到一条数据，只能有该商品的数量会发生变化
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);

        //判断商品是否已经加入到购物车中，而且得要是在某个用户的购物车
        if (list != null && list.size() > 0) {
            //已经在购物车中时只需要该商品数量+1
            ShoppingCart cart = list.get(0);
            cart.setNumber(cart.getNumber() + 1);
            shoppingCartMapper.updateNumById(cart);
        }else{
            //不在购物车里则需要新增商品
            //判断是添加菜品还是套餐
            // TODO 可否从前端直接给出是菜品还是商品？
            Long dishId = shoppingCartDTO.getDishId();
            if(dishId!=null){
                Dish dish = dishMapper.getById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                // TODO 需要根据商品数量来显示单价的倍数吗？还是说由前端完成数量x单价
                shoppingCart.setAmount(dish.getPrice());
            }else{
                Long setmealId = shoppingCartDTO.getSetmealId();
                Setmeal setmeal = setmealMapper.getById(setmealId);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);
        }
    }

    /**
     * 查看购物车
     * @return
     */
    public List<ShoppingCart> show() {
        //获取当前用户的userId
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();
        return shoppingCartMapper.list(shoppingCart);
    }
}
