package com.sky.service;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO);

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    /**
     * 用户端历史订单查询
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult pageQueryForUser(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据订单表的主键去查询某个订单详情
     * @param id
     * @return
     */
    OrderVO detail(Long id);

    /**
     * 根据订单表的主键取消某个订单
     * @param id
     */
    void cancelByUser(Long id) throws Exception;
}
