package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
// TODO 是否用延时队列更能符合理想的设计场景？
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时未支付订单，每分钟查询一次
     */
    @Scheduled(cron = "0 * * * * ? ")
    public void processTimeoutOrder(){
        log.info("定时处理超时未支付订单: {}", LocalDateTime.now());
        // TODO 建议不要频繁地select查询数据库，是否有方法解决频繁select * ?
        // select * from orders where status = ? and orderTime < 当前时间 - 15 min
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, LocalDateTime.now().minusMinutes(15));

        if(ordersList != null && ordersList.size()>0){
            for(Orders order : ordersList){
                order.setStatus(Orders.CANCELLED);
                order.setCancelTime(LocalDateTime.now());
                order.setCancelReason("订单超时，自动取消");
                orderMapper.update(order);
            }
        }
    }

    /**
     * 处理所有在派送中但是已经送达的订单，建议是设计在店铺打烊后一段时间后
     */
    @Scheduled(cron = "0 0 1 * * ? ")
    //@Scheduled(cron = "1/10 * * * * ? ")
    public void processDeliveryOrder(){
        log.info("定时处理派送中订单: {}", LocalDateTime.now());
        // TODO 最理想状态，用户有收到按钮点了后台直接修改状态；用户没点，商家每隔一段时间（比如小时）查询开始派送时间<当前时间-预计派送时间
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, LocalDateTime.now().minusMinutes(60));

        if(ordersList != null && ordersList.size()>0){
            for(Orders order : ordersList){
                order.setStatus(Orders.COMPLETED);
                orderMapper.update(order);
            }
        }
    }
}
