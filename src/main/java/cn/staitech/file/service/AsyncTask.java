package cn.staitech.file.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import cn.staitech.file.service.remote.SlideImageService;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;

/**
 * @author: wangfeng
 * @create: 2023-06-21 14:28:47
 * @Description: 异步Task
 */

//@Component
@Slf4j
@Service
public class AsyncTask {

    @Autowired
    private OpenSlideService openSlideService;
    
    @Resource
    private SlideImageService slideImageService;

    /**
     * 异步生成缩略图
     *
     * @param file
     * @param imageId
     */
    @Async("asyncExecutorThumbImage")
    public void processThumbTask(File file, Long imageId) {
        log.info("异步生成缩略图 :{} {}", imageId, file.getAbsolutePath());
        openSlideService.processThumbUpdate(file, imageId);
    }

    /**
     * 异步删除文件
     *
     * @param file
     * @throws InterruptedException
     */
    @Async("asyncExecutorFailFile")
    public void deleteFileTask(File file) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        AtomicInteger count = new AtomicInteger(0);
        for (; ; ) {
            Thread.sleep(2);
            if (file.delete()) {
                long endTime = System.currentTimeMillis();
                log.info("[{}] async delete file success:{},cost {} ms,cas count:{}", Thread.currentThread().getName(), file.getAbsolutePath(), endTime - startTime, count.getAndIncrement());
                break;
            }
            if (count.getAndIncrement() > 30000) {
                break;
            }
        }
    }

}