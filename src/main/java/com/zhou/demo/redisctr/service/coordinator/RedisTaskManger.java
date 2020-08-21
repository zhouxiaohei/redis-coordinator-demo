package com.zhou.demo.redisctr.service.coordinator;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName RedisTaskManger
 * @Author JackZhou
 * @Date 2020/8/17  16:14
 **/
@Component
@Slf4j
public class RedisTaskManger {

    @Autowired
    private RedisCoordinatorService redisCoordinatorService;

    private Map<TaskConifg, DistributedTask> taskMap = new ConcurrentHashMap<>();

    private int taskMangerPeriod = 30;

    ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

    @PostConstruct
    public void init() {
        CompletableFuture.runAsync(() -> scheduleTask(), Executors.newSingleThreadExecutor());
    }

    public void scheduleTask() {
        while (true) {
            long begin = System.currentTimeMillis();
            try {
                if (taskMap.size() > 0) {
                    execTasks();
                }
                long cost = System.currentTimeMillis() - begin;
                TimeUnit.MILLISECONDS.sleep(taskMangerPeriod * 1000 - cost);
            } catch (Exception e) {
                log.error("RedisTaskManger调度任务出错", e);
            }
        }
    }

    public void execTasks(){
        boolean execFlag = redisCoordinatorService.checkMaster();
        Iterator<Map.Entry<TaskConifg, DistributedTask>> iterator = taskMap.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<TaskConifg, DistributedTask> next = iterator.next();
            TaskConifg conifg = next.getKey();
            conifg.setCount(conifg.getCount() + 1);
            if(conifg.getCount() * conifg.getTaskMangerPeriod() == conifg.getTaskPeriod()){
                conifg.setCount(0);
                if(execFlag){
                    log.info("RedisTaskManger开始调度执行任务：{}",  conifg.getTaskName());
                    CompletableFuture.runAsync(() -> next.getValue().execTask(), executorService);
                }
            }
        }
    }

    public synchronized void addTask(DistributedTask task) {
        Assert.notNull(task, "任务不能为空");
        log.info("添加任务{}, 任务时间间隔{}", task.getTaskName(), task.getPeriodSecond());
        reTaskMangerPeriod(task.getPeriodSecond());
        TaskConifg taskConifg = new TaskConifg();
        taskConifg.setTaskMangerPeriod(taskMangerPeriod);
        taskConifg.setTaskName(task.getTaskName());
        taskConifg.setTaskPeriod(task.getPeriodSecond());
        taskMap.put(taskConifg, task);
    }

    private void reTaskMangerPeriod(int taskPeriod) {
        int taskMangerPeriodOld = taskMangerPeriod;
        if (taskMap.size() == 0) {
            taskMangerPeriod = taskPeriod;
            log.info("RedisTaskManger 设置taskMangerPeriod,由默认{}秒, 更新为{}秒", taskMangerPeriodOld, taskMangerPeriod);
        } else {
            taskMangerPeriod = minMultiple(taskMangerPeriodOld, taskPeriod);
            if (taskMangerPeriodOld != taskMangerPeriod) {
                log.info("RedisTaskManger 设置taskMangerPeriod,由{}秒, 更新为{}秒", taskMangerPeriodOld, taskMangerPeriod);
                reTaskConifg();
            }
        }
    }

    private void reTaskConifg() {
        Set<TaskConifg> taskConifgs = taskMap.keySet();
        for (TaskConifg taskConifg : taskConifgs) {
            int taskMangerPeriodOld = taskConifg.getTaskPeriod();
            int countOld = taskConifg.getCount();
            taskConifg.setCount(taskMangerPeriodOld * countOld / taskMangerPeriod);
            taskConifg.setTaskMangerPeriod(taskMangerPeriod);
        }
    }

    public static int minMultiple(int a, int b) {
        if (a < b) {
            int t = a;
            a = b;
            b = t;
        }
        while (b != 0) {
            if (a == b) {
                return a;
            } else {
                int k = a % b;
                a = b;
                b = k;
            }
        }
        return a;
    }

}

@Data
class TaskConifg{
    private String taskName;
    private int taskPeriod;
    private int taskMangerPeriod;
    private int count;
}