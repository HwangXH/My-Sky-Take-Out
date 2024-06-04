package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    @Autowired
    private UserMapper userMapper;

    //微信支付相关的工具类
    @Autowired
    private WeChatPayUtil weChatPayUtil;


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

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) {
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //跳过不能实现的支付接口调用，直接默认为支付成功

        /*//调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = null;
        try {
            jsonObject = weChatPayUtil.pay(
                    ordersPaymentDTO.getOrderNumber(), //商户订单号
                    new BigDecimal(0.01), //支付金额，单位 元
                    "苍穹外卖订单", //商品描述
                    user.getOpenid() //微信用户的openid
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException(MessageConstant.ORDER_ALREADY_PAYED);
        }*/

        JSONObject jsonObject = new JSONObject();
        //告诉前端已经支付成功
        jsonObject.put("code", "OrderPaid");
        OrderPaymentVO orderPaymentVO = jsonObject.toJavaObject(OrderPaymentVO.class);
        orderPaymentVO.setPackageStr(jsonObject.getString("package"));

        //因为没有涉及paySuccessNotify回调，因此在这里直接修改订单状态
        String orderNumber = ordersPaymentDTO.getOrderNumber();
        paySuccess(orderNumber);

        return orderPaymentVO;
    }

    /**
     * 订单支付成功，修改订单状态，现在无法使用微信支付，所以本质是在payment方法中直接调用
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {
        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED) //待接单
                .payStatus(Orders.PAID) //已支付
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    /**
     * 用户端历史订单分页查询
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult pageQueryForUser(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        long total = page.getTotal();

        //不仅需要订单表，还需要补充订单明细
        List<OrderVO> orderVOList = new ArrayList<>();
        if (page!=null && total >0){
            for (Orders orders : page) {
                Long ordersId = orders.getId();

                //根据1条订单的id去查询订单明细
                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(ordersId);

                OrderVO orderVO = new OrderVO();
                // 因为OrderVO是在Orders实体类的基础上多加了两个属性，orderDishes选择不赋值，赋值orderDetailList
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetailList);

                orderVOList.add(orderVO);
            }
        }

        List<OrderVO> records = orderVOList;

        return new PageResult(total, records);
    }

    /**
     * 根据订单主键去查询某个订单详情
     * @param id
     * @return
     */
    public OrderVO detail(Long id) {
        Orders orders = orderMapper.getById(id);

        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    /**
     * 用户端取消订单，但是订单得要是 待付款 或 待接单的状态
     * @param id
     */
    public void cancelByUser(Long id) throws Exception {
        Orders orders = orderMapper.getById(id);

        //检查订单是否还存在
        if (orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //检查订单是否 属于 待付款或者待接单，不满足则不能取消
        if (orders.getStatus() > 2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //如果是待接单，那么额外操作一下申请退款
        if (orders.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            /*//调用微信支付的退款接口
            weChatPayUtil.refund(
                    orders.getNumber(),
                    orders.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));*/
            //支付状态修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }
        //更新订单状态，取消原因和取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }
}
