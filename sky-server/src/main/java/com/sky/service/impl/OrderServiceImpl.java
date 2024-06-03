package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.OrderService;
import com.sky.vo.OrderSubmitVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;


    /**
     * 用户下单,涉及两个表
     * @param ordersSubmitDTO
     * @return
     */
    //需不需要用事务注解呢？推广一下什么时候用比较好
    //因为插入detail数据时可能发生错误，不加事务注解会让order表有数据，orderDetail表没有数据
    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        //处理业务异常，收获地址不能为空，购物车不能为空，其实前端能够保证购物车为空不能下单
        //不能完全信任前端，因为完全信任的时候会被黑入的请求造成损伤
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if(addressBook==null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartlist = shoppingCartMapper.list(shoppingCart);

        if(shoppingCartlist==null || shoppingCartlist.size()==0){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //向订单表插入1条订单数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);

        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID); // 未支付
        orders.setStatus(Orders.PENDING_PAYMENT); // 待付款
        // TODO 订单号用的时间戳精确到毫秒级，订单号冲突概率极低，但是可以用更保险的方法
        orders.setNumber(String.valueOf(System.currentTimeMillis())); //时间戳作为订单号
        // TODO orders的address没有赋值，但是有AddressBookId，需要把详细地址address补上吗
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee()); //收货人
        orders.setUserId(userId);
        orderMapper.insert(orders);

        Long ordersId = orders.getId(); // 订单id，order表的主键

        //向订单明细表，插入多少由购物车数据确定
        List<OrderDetail> orderDetailList = new ArrayList<OrderDetail>();
        for (ShoppingCart cart : shoppingCartlist) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(ordersId);
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);

        //下单成功后，清空购物车
        shoppingCartMapper.deleteByUserId(userId);

        //封装VO
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(ordersId)
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();

        return orderSubmitVO;
    }
}