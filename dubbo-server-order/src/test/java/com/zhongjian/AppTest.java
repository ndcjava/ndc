package com.zhongjian;


import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import com.zhongjian.dao.jdbctemplate.AddressDao;
import com.zhongjian.dao.jdbctemplate.MarketDao;
import com.zhongjian.dao.jdbctemplate.OrderDao;
import com.zhongjian.dao.jdbctemplate.UserDao;
import com.zhongjian.localservice.OrderService;
import com.zhongjian.service.order.CVOrderService;
import com.zhongjian.task.AddressTask;
import com.zhongjian.util.TaskUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/META-INF/spring/dubbo-server.xml"})
public class AppTest {

    @Resource
    OrderDao orderDao;
    @Resource
    AddressDao addressDao;

    @Resource
    private CVOrderService cvOrderService;
    
    @Autowired
    MarketDao marketDao;

    @Autowired
    AddressTask addressTask;
    @Autowired
    UserDao userDao;


    @Test
    public void test() {
    	cvOrderService.cancelOrder(17);
    	cvOrderService.cancelOrder(18);
    	cvOrderService.cancelOrder(19);
    }

}