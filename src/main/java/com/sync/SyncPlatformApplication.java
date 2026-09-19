package com.sync;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 配置化数据同步平台 - Spring Boot 启动类
 */
@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.data.jdbc.JdbcRepositoriesAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration.class
})
@EnableScheduling       // 支持 Cron 定时任务调度
@MapperScan("com.sync.mapper")
public class SyncPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(SyncPlatformApplication.class, args);
    }
}
