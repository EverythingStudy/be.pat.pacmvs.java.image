package cn.staitech.file.config;

import cn.hutool.core.thread.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import java.util.concurrent.*;

@Slf4j
@Configuration
@EnableAsync
public class ThreadPoolConfig {

    /**
     * OpenSlide处理线程池
     */
    @Bean("openSlideTaskExecutor")
    public ThreadPoolExecutor openSlideTaskExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolExecutor(
                processors + 1,
                processors * 2,
                30,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100000),
                new NamedThreadFactory("OpenSlide-Thread-Pool",false),
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        log.error("OpenSlide任务被拒绝执行: {}", r);
                        // 可以选择使用调用者线程执行
                        if (!executor.isShutdown()) {
                            r.run();
                        }
                    }
                }
        );
    }

    /**
     * Python脚本执行线程池
     */
    @Bean("pythonTaskExecutor")
    public ThreadPoolExecutor pythonTaskExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolExecutor(
                Math.max(1, processors/16), // 确保至少有1个线程
                Math.max(2, processors/16), // 确保至少有2个线程
                30,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100000),
                new NamedThreadFactory("Python-Thread-Pool",false),
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        log.error("Python任务被拒绝执行: {}", r);
                        if (!executor.isShutdown()) {
                            r.run();
                        }
                    }
                }
        );
    }
}
