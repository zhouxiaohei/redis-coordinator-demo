package com.zhou.demo.redisctr.service.coordinator;


public interface DistributedTask {

    void execTask();

    int getPeriodSecond();

    String getTaskName();

}
