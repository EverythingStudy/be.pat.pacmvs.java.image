package cn.staitech.file.vo;

import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2023/11/15 09:23:19
 */
@Slf4j
public class ThreadPoolTest {
    public static void main(String[] args){
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(processors * 2 + 1, processors * 4, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100000));
        for (int i=0;i<100;i++){
            Task task = new Task(i);
            threadPoolExecutor.submit(task);
        }
        threadPoolExecutor.shutdown();

    }
    static class Task implements Runnable{

        private Integer i;

        public Task(Integer i) {
            this.i = i;
        }

        @Override
        public void run() {
            log.info("===================[{}]==[{}]=======================",i,Thread.currentThread().getName());
        }
    }
}
