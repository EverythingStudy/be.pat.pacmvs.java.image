package cn.staitech.file.service.impl;

import cn.staitech.file.constant.ImageConstant;
import cn.staitech.file.domain.Image;
import cn.staitech.file.mapper.ImageMapper;
import cn.staitech.file.service.FileService;
import cn.staitech.file.service.OpenSlideService;
import cn.staitech.file.vo.Chunk;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import cn.staitech.file.util.FileUploadUtils;

/**
 * @author mugw
 * @version 1.0
 * @description 分片文件处理服务实现类
 * @date 2025/4/22 09:32:40
 */
@Slf4j
@Service
public class FileServiceImpl implements FileService {

    public static final Map<Long, AtomicReference<Integer>[]> FILE_MAP_SYN = new ConcurrentHashMap<>();

    @Resource
    private ImageMapper imageMapper;
    @Resource
    private OpenSlideService openSlideService;

    /**
     * 合并文件块
     * <p>
     * 该方法负责将接收到的文件块合并到指定的图像文件中如果图像状态为启用，则直接返回成功
     * 如果文件不存在且未在并发控制图中，则创建新文件并初始化相关控制变量
     * 使用输入流读取文件块内容，并将其写入到目标文件中，同时更新上传进度
     * 当所有文件块上传完成后，更新图像处理状态，并异步生成缩略图
     *
     * @param chunk 文件块对象，包含文件块信息和内容
     * @return 文件块合并结果，成功返回true，失败返回false
     * @throws IOException          如果文件处理过程中发生I/O错误
     * @throws InterruptedException 如果线程被中断
     */
    public boolean mergeChunk(Chunk chunk) throws IOException {
        // 获取图像ID
        Long imageId = chunk.getImageId();
        // 根据图像ID查询图像信息
        Image image = imageMapper.selectById(imageId);
        // 如果图像状态为启用，则直接返回成功
        if (image.getStatus().equals(ImageConstant.IMAGE_STATUS_ENABLE)) {
            return true;
        }
        // 获取图像文件路径
        String path = image.getImagePath();
        File file = new File(path);
        // 文件不存在就创建
        if (!file.exists() && FILE_MAP_SYN.get(imageId) == null) {
            synchronized (FILE_MAP_SYN) {
                if (FILE_MAP_SYN.get(imageId) == null) {
                    try {
                        int totalChunks = chunk.getTotalChunks();
                        if (totalChunks < 0) {
                            log.warn("chunk is : ImageId [{}] ChunkNumber [{}] TotalChunks [{}] Total chunks cannot be negative",chunk.getImageId(), chunk.getChunkNumber(), totalChunks);
                            return false;
                        }
                        //初始化状态集合
                        AtomicReference<Integer>[] chunkStates = new AtomicReference[totalChunks];
                        for (int i = 0; i < totalChunks; i++) {
                            chunkStates[i] = new AtomicReference<>(0);
                        }
                        FILE_MAP_SYN.putIfAbsent(imageId, chunkStates);
                        // 检查并创建目录
                        FileUploadUtils.checkDirectory(file.getAbsolutePath());
                        // 创建新文件
                        file.createNewFile();
                        log.info("chunk is : ImageId [{}] ChunkNumber [{}] TotalChunks [{}],创建文件 [{}] 及分片状态列表成功", chunk.getImageId(), chunk.getChunkNumber(), totalChunks, path);
                    } catch (IOException e) {
                        log.error("chunk is : ImageId [{}] ChunkNumber [{}] TotalChunks [{}] 创建文件时异常",chunk.getImageId(), chunk.getChunkNumber(), e.getMessage());
                        if (log.isDebugEnabled()){
                            e.printStackTrace();
                        }
                        // 清理无效条目
                        FILE_MAP_SYN.remove(imageId);
                        FileUtils.delete(file);
                        return false;
                    }
                }
            }
        }
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
            while ((len = fis.read(buffer)) != -1) {
                raf.write(buffer, 0, len);
            }
            log.debug("chunk is : ImageId [{}] ChunkNumber [{}] TotalChunks [{}],分片写入完成", chunk.getImageId(), chunk.getChunkNumber(), chunk.getTotalChunks());
        } catch (IOException e) {
            log.error("chunk is : ImageId [{}] ChunkNumber [{}] TotalChunks [{}],分片写入异常：[{}]", chunk.getImageId(), chunk.getChunkNumber(), chunk.getTotalChunks(), e.getMessage());
            return false;
        }
        // 更新并发控制变量
        AtomicReference<Integer>[] chunkStates = FILE_MAP_SYN.get(imageId);
        int chunkNumber = chunk.getChunkNumber();
        chunkStates[chunkNumber].compareAndSet(0, 1);
        log.debug("chunk is : ImageId [{}] ChunkNumber [{}] TotalChunks [{}],更新分片状态列表 [{}]", chunk.getImageId(), chunk.getChunkNumber(), chunk.getTotalChunks(),chunk);
        int temp = Arrays.stream(chunkStates).collect(Collectors.toList()).stream().filter(i -> i.get() == 1).mapToInt(i -> 1).sum();
        log.info("chunk is : ImageId [{}] ChunkNumber [{}] TotalChunks [{}],分片文件大小:[{}]，上传进度:[{}/{}]", chunk.getImageId(), chunk.getChunkNumber(), chunk.getTotalChunks(), chunk.getChunkSize(), temp, chunk.getTotalChunks());
        // 当所有文件块上传完成后，更新图像处理状态，并异步生成缩略图
        if (temp == chunk.getTotalChunks()) {
            image.setProcessFlag(ImageConstant.IMAGE_PROCESS_PARSING);
            imageMapper.updateById(image);
            log.debug("chunk is : ImageId [{}] ChunkNumber [{}] TotalChunks [{}], 开始解析原始切片", chunk.getImageId(), chunk.getChunkNumber(), chunk.getTotalChunks());
            FILE_MAP_SYN.remove(imageId);
            try {
                openSlideService.processThumb(image);
            } catch (Exception e) {
                log.error("chunk is : ImageId [{}] ChunkNumber [{}] TotalChunks [{}],生成缩略图异常：[{}]", chunk.getImageId(), chunk.getChunkNumber(), chunk.getTotalChunks(), e.getMessage());
                if (log.isDebugEnabled()){
                    e.printStackTrace();
                }
            }
            // 异步计算MD5
            // asyncTask.calculateMd5(path, imageId);
        }
        return true;
    }
}
