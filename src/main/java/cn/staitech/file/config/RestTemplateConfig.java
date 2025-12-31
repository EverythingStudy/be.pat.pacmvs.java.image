package cn.staitech.file.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author admin
 * @version 1.0
 * @since 2025/12/31
 */
@Configuration
public class RestTemplateConfig {

    @LoadBalanced  // 关键注解，启用负载均衡
    @Bean
    public RestTemplate restTemplate() {
        // 创建连接工厂
        HttpComponentsClientHttpRequestFactory factory =
            new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(10000);           // 连接超时
        factory.setReadTimeout(30000);             // 读取超时
        factory.setConnectionRequestTimeout(10000); // 请求超时

        // 创建RestTemplate实例
        RestTemplate restTemplate = new RestTemplate(factory);

        // 添加消息转换器
        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        jsonConverter.setSupportedMediaTypes(Arrays.asList(
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_JSON_UTF8
        ));
        messageConverters.add(jsonConverter);
        restTemplate.setMessageConverters(messageConverters);

        return restTemplate;
    }
}

