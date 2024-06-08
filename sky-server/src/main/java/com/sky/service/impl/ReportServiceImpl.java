package com.sky.service.impl;

import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 统计时间区间内的营业额
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //把所有日期和营业额都加入到一个list中,因为只是商家界面粗糙的图形统计，因此不必用BigDecimal
        List<LocalDate> dateList = new ArrayList<>();
        List<Double> turnoverList = new ArrayList<>();

        dateList.add(begin);
        // TODO 是否需要检验一下输入日期是否合理，begin要在end前
        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //营业额计算
        //应该要避免在一些常用的循环里读取数据库
        // TODO 是否可以用group by方法返回不同日期的结果,或者还有什么方法可以降低开销，比如SQL查询时用like方法选取下单时间
        for (LocalDate date : dateList) {
            //营业是 已完成 订单的金额总计

            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN); //该天的00：00
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX); //该天的23：59：59...
            //select sum(amount) from orders where order_time > ? and order_time < ? and status = 5
            Map map = new HashMap<>();
            map.put("begin", beginTime);
            map.put("end", endTime);
            Double turnover = orderMapper.sumByMap(map);
            //如果某天没有营业额应该避免返回的是null
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }

        //把list转成字符串，分割符逗号
        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }
}
