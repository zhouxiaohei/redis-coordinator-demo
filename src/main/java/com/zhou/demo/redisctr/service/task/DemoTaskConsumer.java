package com.zhou.demo.redisctr.service.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName DemoTaskConsumer
 * @Author JackZhou
 * @Date 2020/8/18  11:31
 **/
@Component
@Slf4j
public class DemoTaskConsumer {

    @Autowired
    private RedisTemplate redisTemplate;

    ThreadPoolExecutor executor = null;

    @Value("${demo.task.consumerPool}")
    private int consumerPool;

    @PostConstruct
    public void init() {
        executor = new ThreadPoolExecutor(consumerPool, consumerPool, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        CompletableFuture.runAsync(() -> onMessage(), Executors.newSingleThreadExecutor());
    }

    public void onMessage() {
        int customerWaitCount = 0;
        while (true) {
            try {
                checkExecPool();
                String taskId = (String)redisTemplate.opsForList().leftPop("test123List", 10, TimeUnit.SECONDS);
                if (taskId != null) {
                    customerWaitCount = 0;
                    log.info("接到任务 {}", taskId);
                    CompletableFuture.runAsync( () -> exec(taskId), executor);
                } else {
                    customerWaitCount++;
                    if (customerWaitCount > 20) {
                        log.warn("警告,{}秒没有接到新任务了!", 10 * customerWaitCount);
                    }
                }
            } catch (Exception e) {
                log.error("接收数据处理任务出错", e);
            }
        }
    }

    private void exec(String taskId){
        log.info("执行任务：{}", taskId );
    }

    private void checkExecPool(){
        try{
            int poolWatiCount = 0;
            while (executor.getQueue().size() > 0) {
                if (poolWatiCount > 100) {
                    log.warn("任务拥挤中,当前任务队列数量：{}", executor.getQueue().size());
                }
                poolWatiCount++;
                TimeUnit.SECONDS.sleep(poolWatiCount < 10 ? 1 : 2);
            }
        }catch (Exception e){
            log.error("check execPool error", e);
        }

    }

}
