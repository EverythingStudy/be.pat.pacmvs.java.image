package cn.staitech.file.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class AsyncTask {

    @Resource
    private OpenSlideService openSlideService;

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