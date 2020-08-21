package com.zhou.demo.redisctr;

import com.zhou.demo.redisctr.service.coordinator.RedisTaskManger;
import com.zhou.demo.redisctr.service.task.DemoTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @ClassName MyStartupRunner
 * @Author JackZhou
 * @Date 2020/8/3  11:11
 **/
@Component
@Order(1)
@Slf4j
public class MyStartupRunner implements CommandLineRunner {

    @Autowired
    RedisTaskManger redisTaskManger;

    @Autowired
    DemoTask demoTask;

    @Override
    public void run(String... args) throws Exception {
        redisTaskManger.addTask(demoTask);
    }

}
