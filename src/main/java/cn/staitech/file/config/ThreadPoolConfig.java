package cn.staitech.file.config;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

/**
 * 线程池配置 https://blog.csdn.net/IT_road_qxc/article/details/123090678
 *
 * @author: wangfeng
 * @create: 2023-07-31 11:30:17
 * @Description: ThreadPoolConfig
 */

@Configuration
@EnableAsync
@Slf4j
public class ThreadPoolConfig {

    /**
     * 创建线程池 用于正常采集搬迁的文件 上传
     */
    @Bean(name = "asyncExecutorThumbImage")
    public Executor asyncExecutorThumbImage() {
        return ThreadUtil.newExecutor();
    }

    /**
     * 创建线程池 用于处理失败文件删除-线程池
     */
    @Bean(name = "asyncExecutorFailFile")
    public Executor asyncExecutorFailFile() {
        return ThreadUtil.newExecutor();
    }
    
    /**
     * 创建线程池 用于操作日志处理-线程池
     */
    @Bean(name = "asyncExecutorOperationLog")
    public Executor asyncExecutorOperationLog() {
        return ThreadUtil.newExecutor();
    }
}
