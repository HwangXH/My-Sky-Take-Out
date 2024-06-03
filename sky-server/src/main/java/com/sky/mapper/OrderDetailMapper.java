package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrderDetailMapper {

    /**
     * 批量插入，传入的是list，需要用动态foreach来插入
     * @param orderDetailList
     */
    void insertBatch(List<OrderDetail> orderDetailList);
}
