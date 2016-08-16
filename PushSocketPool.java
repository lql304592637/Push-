package com.sophlean.rmes.service.push;

import com.sophlean.core.util.log.Log;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by lql on 2016/4/22.
 */
public class PushSocketPool {
    private static int defaultNum = 100;
    private static ThreadPoolTaskExecutor pushPool = null;
    private static String confFile = "D:\\PoolConfig.properties";
    private static FileInputStream fileLoad = null;
    private static Properties config = null;
    protected static final Log log = Log.getLog(PushSocketPool.class);

    static {
        try {
            fileLoad = new FileInputStream(confFile);
            config = new Properties();
            config.load(fileLoad);
            pushPool = new ThreadPoolTaskExecutor();
            pushPool.setCorePoolSize(Integer.parseInt(config.getProperty("minNum")));
            pushPool.setMaxPoolSize(Integer.parseInt(config.getProperty("maxNum")));
            pushPool.setQueueCapacity(Integer.parseInt(config.getProperty("coreNum")));
            pushPool.setKeepAliveSeconds(Integer.parseInt(config.getProperty("aliveTime")));
            pushPool.initialize();
            log.debug(confFile + "加载成功");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public static void doPush(Runnable push) {
        pushPool.execute(push);
    }

    public static int getDefaultNum() {
        return defaultNum;
    }

    public static void setDefaultNum(int defaultNum) {
        PushSocketPool.defaultNum = defaultNum;
    }

}
