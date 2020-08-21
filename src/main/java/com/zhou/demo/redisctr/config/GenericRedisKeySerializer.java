package com.zhou.demo.redisctr.config;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.Assert;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @ClassName GenericRedisKeySerializer
 * @Author JackZhou
 * @Date 2020/8/17  10:35
 **/
public class GenericRedisKeySerializer implements RedisSerializer<Object> {

    private final Charset charset;
    private String prefix;
    private int index;

    public GenericRedisKeySerializer(String prefix) {
        this(prefix, StandardCharsets.UTF_8);
    }

    public GenericRedisKeySerializer(String prefix, Charset charset) {
        Assert.notNull(charset,"字符集不可为空");
        Assert.notNull(prefix, "前缀不可为空");
        this.charset = charset;
        this.prefix = prefix + ":";
        index = this.prefix.length();
    }

    @Override
    public String deserialize(byte[] bytes) {

        if (null == bytes) {
            return null;
        }
        String key = new String(bytes, charset);
        if (key.indexOf(prefix) == 0) {
            return key.substring(index, key.length());
        }
        return key;
    }

    @Override
    public byte[] serialize(Object key) {

        if (null == key) {
            return null;
        }
        String string = key.toString();
        if (!string.startsWith(prefix)) {
            string = prefix + string;
        }
        return string.getBytes(charset);
    }

}
