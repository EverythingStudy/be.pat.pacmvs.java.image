package cn.staitech.file.service.impl;

import cn.staitech.file.constant.ImageConstant;
import cn.staitech.file.domain.Image;
import cn.staitech.file.mapper.ImageMapper;
import cn.staitech.file.service.AsyncTask;
import cn.staitech.file.service.FileService;
import cn.staitech.file.util.Container;
import cn.staitech.file.util.ImageUtils;
import cn.staitech.file.vo.Chunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;
import cn.staitech.file.util.FileUploadUtils;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class FileServiceImpl implements FileService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ImageMapper imageMapper;

    @Resource
    private AsyncTask asyncTask;

    /**
     * 合并切片(并发问题)
     *
     * @param chunk
     * @return
     * @throws IOException
     */
    @Transactional
    public boolean mergeChunk(Chunk chunk) throws IOException, InterruptedException {
        // log.info("------> chunk:{}", chunk);
        Long imageId = chunk.getImageId();
        // 取文件路径
        //String cacheKey = ImageUtils.getPathKey(String.valueOf(imageId));
        //String path = stringRedisTemplate.opsForValue().get(cacheKey);
        Image image = imageMapper.selectById(imageId);
        if (image.getStatus().equals(ImageConstant.IMAGE_STATUS_ENABLE)){
            return true;
        }
        String path = image.getImagePath();
        File file = new File(path);
        // 文件不存在就创建
        if (!file.exists() && !Container.FILE_MAP_SYN.containsKey(imageId)) {
            try {
                // log.info("路径为: " + file.getAbsolutePath() + " 开始创建");
                AtomicInteger atomicInteger = new AtomicInteger();
                atomicInteger.set(chunk.getTotalChunks());
                Container.FILE_MAP_SYN.put(imageId,atomicInteger);
                // log.info("create new file ---> filepath:{}, file hash code:{}", file.getAbsolutePath(), file.getAbsolutePath().hashCode());
                // 没有文件夹，则创建文件夹*****
                FileUploadUtils.checkDirectory(file.getAbsolutePath());
                file.createNewFile();
            } catch (IOException e) {
                log.error("创建文件时异常:{}", e);
                return false;
            }
        }
        // log.error("hunk.getFile().hashCode():{}", chunk.getFile().hashCode());
        // 将块文件写入文件中
        try (InputStream fis = chunk.getFile().getInputStream();
             RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            int len = -1;
            byte[] buffer = new byte[1024 * 4 * 10];
            // 指针移动到当前块开始写的位置，chunk.getChunkNumber()是指当前是第几块，减一后乘
            // 以每个块的大小 得到前面块的偏移量，即当前块的起始位置
            raf.seek((chunk.getChunkNumber()) * chunk.getChunkSize());
            //log.info("------> seek:{}", (chunk.getChunkNumber()) * chunk.getChunkSize());
            //把当前块的内容写入
            // java.util.ConcurrentModificationException: null,并发修改异常
            while ((len = fis.read(buffer)) != -1) {
                raf.write(buffer, 0, len);
            }
        } catch (IOException e) {
            log.error("上传大图像io异常：[{}]",e.getMessage());
            // 异步删除文件
            asyncTask.deleteFileTask(file);
            return false;
        }
        AtomicInteger atomicInteger =  Container.FILE_MAP_SYN.get(imageId);
        int temp = atomicInteger.decrementAndGet();
        log.info("上传大图像ImageId:[{}]，分片文件序号:[{}]，分片文件大小:[{}]，上传进度:[{}/{}]",chunk.getImageId(),chunk.getChunkNumber(),chunk.getChunkSize(),temp,chunk.getTotalChunks());
        if (Container.FILE_MAP_SYN.get(imageId) != null && temp==0) {
            image.setProcessFlag(ImageConstant.IMAGE_PROCESS_PARSING);
            imageMapper.updateById(image);
            Container.FILE_MAP_SYN.remove(imageId);
            log.info("异步生成缩略图");
            File thisFile = new File(path);
            log.info("----------------------原图文件大小：{}", thisFile.length());
            // 异步生成缩略图
            asyncTask.processThumbTask(new File(path), imageId);
            // 异步计算MD5
            // asyncTask.calculateMd5(path, imageId);
        }
        return true;
    }
}
