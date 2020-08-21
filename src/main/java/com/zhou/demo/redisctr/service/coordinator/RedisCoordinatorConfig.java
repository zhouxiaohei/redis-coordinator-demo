package com.zhou.demo.redisctr.service.coordinator;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @ClassName RedisCoordinatorConfig
 * @Author JackZhou
 * @Date 2020/8/17  11:25
 **/

@ConfigurationProperties(prefix = "redis.coordinator")
@Data
@Component
public class RedisCoordinatorConfig {
    private Boolean enabled;
    private String instanceName;
    private int registerPeriod;
    private int renewalPeriod;
}
