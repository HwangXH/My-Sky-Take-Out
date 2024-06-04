package com.sky.service;

import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
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
     * 用户根据订单表的主键取消某个订单
     * @param id
     */
    void cancelByUser(Long id) throws Exception;

    /**
     * 根据订单历史的主键再来一单
     * @param id
     */
    void repetition(Long id);

    /**
     * 管理端根据条件搜索订单
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult pageSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 统计各个状态的订单数量
     * @return
     */
    OrderStatisticsVO statistics();

    /**
     * 商家接单
     * @param ordersConfirmDTO
     */
    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    /**
     * 商家拒单
     * @param ordersRejectionDTO
     */
    void reject(OrdersRejectionDTO ordersRejectionDTO) throws Exception;

    /**
     * 商家取消订单
     * @param ordersCancelDTO
     */
    void cancelByAdmin(OrdersCancelDTO ordersCancelDTO) throws Exception;

    /**
     * 商家开始派送
     * @param id
     */
    void delivery(Long id);

    /**
     * 商家完成订单
     * @param id
     */
    void complete(Long id);
}
