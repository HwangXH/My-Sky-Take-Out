package com.sky.controller.admin;

import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Api(tags = "管理端订单接口")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    //用在管理端订单管理页面，如果查看全部订单，前端不会显示订单的详细菜品字符串，如果根据订单状态搜索前端会把详细菜品显示出来
    @GetMapping("/conditionSearch")
    @ApiOperation("订单搜索")
    public Result<PageResult> pageSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        log.info("管理端搜索订单: {}", ordersPageQueryDTO);
        PageResult pageResult = orderService.pageSearch(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    //主要关注待接单、待派送和派送中的订单
    @GetMapping("/statistics")
    @ApiOperation("统计各个状态的订单数量")
    public Result<OrderStatisticsVO> statistics() {
        log.info("统计各个状态的订单数量");
        OrderStatisticsVO orderStatisticsVO = orderService.statistics();
        return Result.success(orderStatisticsVO);
    }

    @GetMapping("/details/{id}")
    @ApiOperation("查询订单详情")
    public Result<OrderVO> detail(@PathVariable Long id) {
        log.info("管理端查询订单详情: 订单主键 {}",id);
        OrderVO orderVO = orderService.detail(id);
        return Result.success(orderVO);
    }

    //商家将订单状态修改为已结单的过程
    @PutMapping("/confirm")
    @ApiOperation("接单")
    public Result confirm(@RequestBody OrdersConfirmDTO ordersConfirmDTO) {
        log.info("商家接单，订单id: {}",ordersConfirmDTO.getId());
        orderService.confirm(ordersConfirmDTO);
        return Result.success();
    }

    //拒单只能发生在待接单时
    @PutMapping("/rejection")
    @ApiOperation("拒单")
    public Result reject(@RequestBody OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        log.info("商家拒单，订单id: {}",ordersRejectionDTO.getId());
        orderService.reject(ordersRejectionDTO);
        return Result.success();
    }

    @PutMapping("/cancel")
    @ApiOperation("取消订单")
    public Result cancel(@RequestBody OrdersCancelDTO ordersCancelDTO) throws Exception {
        log.info("商家取消订单，订单主键: {}",ordersCancelDTO.getId());
        orderService.cancelByAdmin(ordersCancelDTO);
        return Result.success();
    }

    @PutMapping("/delivery/{id}")
    @ApiOperation("派送订单")
    public Result delivery(@PathVariable Long id) {
        log.info("商家开始派送订单，订单id: {}", id);
        orderService.delivery(id);
        return Result.success();
    }

    // TODO 商家完成订单但是前端显示已完成的订单还有可取消的选项，用户在完成的订单中还有申请退款的选项，是否合理
    @PutMapping("/complete/{id}")
    @ApiOperation("完成订单")
    public Result complete(@PathVariable Long id) {
        log.info("商家完成订单，订单主键: {}",id);
        orderService.complete(id);
        return Result.success();
    }
}
