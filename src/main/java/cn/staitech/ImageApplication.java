package cn.staitech;

import cn.staitech.common.security.annotation.EnableCustomConfig;
import cn.staitech.common.security.annotation.EnableRyFeignClients;
import cn.staitech.common.swagger.annotation.EnableCustomSwagger2;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import java.util.TimeZone;

/**
 * @author mugw
 * @version 1.0
 * @description 原始切片解析服务
 * @date 2023/9/8 15:49:31
 */

@EnableWebSocket
@EnableCustomConfig
@EnableCustomSwagger2
@EnableRyFeignClients
@SpringBootApplication
@EnableElasticsearchRepositories(basePackages = {"cn.staitech.common.log.elasticsearchRepositories"})
@EnableDiscoveryClient
@EnableFeignClients
@EnableTransactionManagement
public class ImageApplication {

    public static void main(String[] args)
    {
        //jvm参数设置时间 -Duser.timezone="Asia/Shanghai"
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        SpringApplication.run(ImageApplication.class, args);
        System.out.println("原始切片解析服务启动成功");
    }

    @Bean
    MeterRegistryCustomizer<MeterRegistry> configurer() {
        return registry -> registry.config().commonTags("application", "staitech-image");
    }

}
