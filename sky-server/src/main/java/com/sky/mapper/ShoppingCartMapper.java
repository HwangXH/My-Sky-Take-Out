package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {

    /**
     * 根据给定条件的商品条件，查询得到符合条件的购物车商品列表
     * @param shoppingCart
     * @return
     */
    List<ShoppingCart> list(ShoppingCart shoppingCart);

    /**
     * 根据商品在表中的主键id来增加数量1
     * @param cart
     */
    //这个注解可以从参数中提出取number和id嘛？？
    @Update("update shopping_cart set number = #{number} where id = #{id}")
    void updateNumById(ShoppingCart cart);

    /**
     * 插入购物车数据（一件商品，数量为1）
     * @param shoppingCart
     */
    @Insert("insert into shopping_cart (name, image, user_id, dish_id, setmeal_id, dish_flavor, amount, create_time) " +
            "values (#{name}, #{image}, #{userId}, #{dishId}, #{setmealId}, #{dishFlavor}, #{amount}, #{createTime})")
    void insert(ShoppingCart shoppingCart);
}
