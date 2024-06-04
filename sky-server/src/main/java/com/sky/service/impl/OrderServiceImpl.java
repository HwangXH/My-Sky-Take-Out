package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
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
     * 用户下单,涉及两个表，订单此时为 待付款，未支付
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
     * 订单支付，订单为  待接单，已支付
     * @param ordersPaymentDTO
     * @return
     */
    //是否应该保证 只有支付成功的订单的状态才会由未付款变为待接单？
    @Transactional
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
                BeanUtils.copyProperties(orders, orderVO);
                // 因为OrderVO是在Orders实体类的基础上多加了两个属性，orderDishes选择不赋值，赋值orderDetailList
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
        // TODO 前端用户给出取消订单理由时，可以用OrderCancelDTO来封装订单id和取消理由
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 根据订单历史的主键再来一单
     * @param id
     */
    public void repetition(Long id) {
        Long userId = BaseContext.getCurrentId();

        //先清楚当前购物车内的所有的商品
        shoppingCartMapper.deleteByUserId(userId);

        //根据订单id在明细表中查询订单明细
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        //生成新的购物车条目
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            //对于订单详情的每一条，都生成一次新的购物车内容，属性拷贝（不拷贝明细表的主键）
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).toList();

        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 管理端订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult pageSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        //不仅需要订单信息，还需要订单里的菜品信息，因此要用VO返回
        List<OrderVO> orderVOList = new ArrayList<>();
        if (page!=null && page.getTotal() >0){
            for (Orders orders : page) {
                Long ordersId = orders.getId();

                //根据1条订单的id去查询订单明细
                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(ordersId);
                //根据要求，商品转成商品名x数量的字符串显示
                List<String> orderDishList = orderDetailList.stream().map(x -> {
                    return x.getName() + "*" + x.getNumber() + ";";
                }).toList();
                String orderDishes = String.join("", orderDishList);


                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                // 这次选择赋值给VO中的订单菜品信息字符串orderDishes字段
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }

        return new PageResult(page.getTotal(), orderVOList);
    }

    /**
     * 统计各个状态的订单数量
     * @return
     */
    public OrderStatisticsVO statistics() {

        OrderStatisticsVO orderStatisticsVO = OrderStatisticsVO.builder()
                .toBeConfirmed(orderMapper.countStatus(Orders.TO_BE_CONFIRMED))
                .confirmed(orderMapper.countStatus(Orders.TO_BE_CONFIRMED))
                .deliveryInProgress(orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS))
                .build();
        return orderStatisticsVO;
    }

    /**
     * 商家接单
     * @param ordersConfirmDTO
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Long ordersId = ordersConfirmDTO.getId();

        //修改订单表的订单信息
        Orders orders = Orders.builder()
                .id(ordersId)
                .status(Orders.CONFIRMED) //已接单
                .build();
        orderMapper.update(orders);
    }

    /**
     * 商家拒单
     * @param ordersRejectionDTO
     */
    public void reject(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        //只有订单是待接单的情况才能拒单
        Orders orders = orderMapper.getById(ordersRejectionDTO.getId());

        if(orders == null || !orders.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //如果是待接单的订单，那一定是已经支付的了，要退款
        //但是保险起见
        if (orders.getPayStatus().equals(Orders.PAID)){
            /*String refund = weChatPayUtil.refund(
                    orders.getNumber(),
                    orders.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));*/
            log.info("商家拒单，申请退款");
        }

        //拒单后，更新订单信息
        Orders ordersNew = Orders.builder()
                .id(orders.getId())
                .status(Orders.CANCELLED)
                .payStatus(Orders.REFUND)
                .rejectionReason(ordersRejectionDTO.getRejectionReason())
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(ordersNew);
    }

    /**
     * 商家取消订单
     * @param ordersCancelDTO
     */
    public void cancelByAdmin(OrdersCancelDTO ordersCancelDTO) throws Exception {
        Orders orders = orderMapper.getById(ordersCancelDTO.getId());

        //检查订单是否还存在
        if (orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //如果是待接单，那么额外操作一下申请退款
        if (orders.getStatus() < orders.COMPLETED ){
            if (orders.getPayStatus().equals(Orders.PAID)){
                //调用微信支付的退款接口
                /*weChatPayUtil.refund(
                    orders.getNumber(),
                    orders.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));*/
                log.info("商家取消订单，申请退款");
            }
            //支付状态修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }
        //更新订单状态，取消原因和取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 商家开始派送
     * @param id
     */
    public void delivery(Long id) {
        Orders orders = orderMapper.getById(id);

        if (orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //只有已接单的订单才会派送
        if(!orders.getStatus().equals(Orders.CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.update(orders);
    }

    /**
     * 商家完成订单
     * @param id
     */
    public void complete(Long id) {
        Orders orders = orderMapper.getById(id);

        if (orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //只有已接单的订单才会派送
        if(!orders.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());
        orderMapper.update(orders);
    }
}
