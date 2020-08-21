package com.zhou.demo.redisctr.service.coordinator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @ClassName RedisCoordinatorService
 * @Author JackZhou
 * @Date 2020/8/17  10:19
 **/
@Component
@Slf4j
public class RedisCoordinatorService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedisCoordinatorConfig config;

    private static final String MASTER_KEY = "redisCoordinatorLock";

    private AtomicBoolean isMaster = new AtomicBoolean();

    private ScheduledExecutorService scheduled;

    @PostConstruct
    public void init() {
        config.toString();
        log.info("打印redis协调器配置:{}", config.toString());
        CompletableFuture.runAsync(() -> registerMaster(), Executors.newSingleThreadExecutor());
    }

    private void registerMaster() {
        while (true) {
            try {
                Boolean flag = setMasterLock();
                if (isMaster.get() || flag) {
                    if (flag) {
                        log.info("实例：{},通过线程{}成为主节点", config.getInstanceName(), Thread.currentThread().getId() + Thread.currentThread().getName());
                        isMaster.set(true);
                        renewalNode();
                    } else {
                        boolean checkFlag = checkMaster();
                        if (!checkFlag) {
                            log.error("实例：{}, 主节点下线，成为普通节点", config.getInstanceName());
                            isMaster.set(false);
                            stopRenewal();
                        }
                    }
                }
                TimeUnit.SECONDS.sleep(config.getRegisterPeriod());
            } catch (InterruptedException e) {
                log.error("获取主节点出现异常, 请及时处理", e);
            }
        }
    }

    private void renewalNode(){
        stopRenewal();
        scheduled = Executors.newSingleThreadScheduledExecutor();
        scheduled.scheduleAtFixedRate(() -> {
            renewal();
        }, config.getRenewalPeriod(), config.getRenewalPeriod(), TimeUnit.SECONDS);
    }

    private void renewal(){
        Boolean flag = checkAndReexpire();
        if(!flag){
            log.warn("节点{}续期失败", config.getInstanceName());
            //scheduled.shutdownNow();
        }else{
            log.info("节点{}续期成功", config.getInstanceName());
        }
    }

    private void stopRenewal(){
        if(scheduled != null){
            scheduled.shutdownNow();
        }
    }

    public boolean checkMaster(){
        String  masterInstanceName = (String)redisTemplate.opsForValue().get(MASTER_KEY);
        if(masterInstanceName != null && masterInstanceName.equalsIgnoreCase(config.getInstanceName())){
            return true;
        }
        return false;
    }

    private Boolean setMasterLock(){
        return redisTemplate.opsForValue().setIfAbsent(MASTER_KEY, config.getInstanceName(), config.getRegisterPeriod(), TimeUnit.SECONDS);
    }


    /**
      * @Author JackZhou
      * @Description TODO  应该检查锁是你的锁以后再续期
     **/
    private Boolean checkAndReexpire(){
        if(checkMaster()){
            return redisTemplate.opsForValue().setIfPresent(MASTER_KEY,  config.getInstanceName(), config.getRegisterPeriod(), TimeUnit.SECONDS);
        }
        return false;
//        redisTemplate.execute(new SessionCallback<Object>() {
//
//            @Override
//            public Object execute(RedisOperations redisOperations) throws DataAccessException {
//                try{
//                    redisOperations.watch(MASTER_KEY);
//                }catch (Exception e){
//                    log.error("检查和更新出现问题");
//
//                }
//
//                return null;
//            }
//        });
    }

}

