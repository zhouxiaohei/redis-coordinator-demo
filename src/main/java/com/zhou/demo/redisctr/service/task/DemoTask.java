package com.zhou.demo.redisctr.service.task;

import com.zhou.demo.redisctr.service.coordinator.DistributedTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @ClassName DemoTask
 * @Author JackZhou
 * @Date 2020/8/18  11:28
 **/
@Component
@Slf4j
public class DemoTask implements DistributedTask {

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${demo.task.taskName}")
    private String taskName;

    @Value("${demo.task.periodSecond}")
    private int taskPeriod;

    @Override
    public void execTask() {
        try {
            log.info("任务{}开始执行", getTaskName());
            Set<String> members = redisTemplate.opsForSet().members("test123");
            long taskTime = System.currentTimeMillis();
            List<String> list = new ArrayList();
            for (String member : members) {
                list.add(member + "&" + taskTime);
            }
            redisTemplate.opsForList().leftPushAll("test123List", list);
            log.info("任务{}执行完成", getTaskName());
        } catch (Exception e) {
            log.error("执行任务出错", e);
        }
    }

    @Override
    public int getPeriodSecond() {
        return taskPeriod;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }
}
