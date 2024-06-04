package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderDetailMapper {

    /**
     * 批量插入，传入的是list，需要用动态foreach来插入
     * @param orderDetailList
     */
    void insertBatch(List<OrderDetail> orderDetailList);

    /**
     * 根据订单id查询该订单的所有明细，即这个订单的所有商品信息，一对多
     * @param ordersId
     * @return
     */
    @Select("select * from order_detail where order_id = #{ordersId}")
    List<OrderDetail> getByOrderId(Long ordersId);
}
