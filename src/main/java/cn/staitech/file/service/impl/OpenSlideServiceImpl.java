package cn.staitech.file.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.staitech.file.constant.ImageConstant;
import cn.staitech.file.domain.Image;
import cn.staitech.file.mapper.ImageMapper;
import cn.staitech.file.service.ImageService;
import cn.staitech.file.service.OpenSlideService;
import cn.staitech.file.util.ImageUtils;
import cn.staitech.file.vo.ImageConversionsionResp;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.openslide.OpenSlide;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2025/4/22 09:32:40
 */
@Slf4j
@Service
public class OpenSlideServiceImpl implements OpenSlideService {

    private final String imgType = "jpg";

    private final Integer imgSize = 256;

    @Value("${file.path}")
    private String localFilePath;
    @Value("${pythonScriptPath:/home/staitech/tile.py}")
    private String pythonScriptPath;
    @Value("${pythonExecutable:python}")
    private String pythonExecutable;

    @Resource
    private ImageService imageService;

    @Resource
    private ImageMapper imageMapper;

    private static final ThreadPoolExecutor THREAD_POOL_EXECUTOR;
    private static final ThreadPoolExecutor FIXED_THREAD_POOL_EXECUTOR;


    static {
        int processors = Runtime.getRuntime().availableProcessors();
        THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
                processors + 1,
                processors * 2,
                30,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100000),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        int threadCount = 0;
                        Thread t = new Thread(r, "OpenSlide-Thread-" + threadCount++);
                        t.setPriority(Thread.MAX_PRIORITY); // 设置线程优先级
                        t.setDaemon(false); // 设置是否为守护线程
                        return t;
                    }
                },
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        // 丢弃任务，不抛出异常
                        log.error("OpenSlide THREAD_POOL_EXECUTOR rejectedExecution: {}", r);
                    }
                }
        );
        FIXED_THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
                processors/4,
                processors/4,
                30,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100000),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        int threadCount = 0;
                        Thread t = new Thread(r, "Python-Thread-" + threadCount++);
                        t.setPriority(Thread.MAX_PRIORITY); // 设置线程优先级
                        t.setDaemon(false); // 设置是否为守护线程
                        return t;
                    }
                },
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        // 丢弃任务，不抛出异常
                        log.error("Python THREAD_POOL_EXECUTOR rejectedExecution: {}", r);
                    }
                }
        );
    }

    /**
     * 总层数小于2为不可用
     */
    private final int MIN_LEVEL_COUNT = 2;

    /**
     * 根据物理地址,获取到病理图片的resolutionX,resolutionY,sourceLens并存入image
     *
     * @param os
     * @param image
     * @return
     * @throws IOException
     */
    private void writeRseolution(OpenSlide os, Image image) {
        String mppX = "";
        String mppY = "";
        Integer sourceLens = 0;
        Map<String, String> properties = os.getProperties();

        // 获取原始图像参数
        if (properties.containsKey("openslide.mpp-x")) {
            mppX = properties.get("openslide.mpp-x");
        }

        if (properties.containsKey("openslide.mpp-y")) {
            mppY = properties.get("openslide.mpp-y");
        }

        if (properties.containsKey("openslide.objective-power")) {
            // SVS
            sourceLens = Integer.valueOf(properties.get("openslide.objective-power"));
        } else if (properties.containsKey("hamamatsu.SourceLens")) {
            // NDPI
            sourceLens = Integer.valueOf(properties.get("hamamatsu.SourceLens"));
        }

        // 特殊处理生仝算法组导出的图像
        if (mppX == "" && mppY == "" && sourceLens == 0 && properties.containsKey("tiff.ImageDescription")) {
            String imageDescription = properties.get("tiff.ImageDescription");
            if (imageDescription != null) {
                JSONObject jsonObject = JSON.parseObject(imageDescription);
                if (jsonObject.containsKey("openslide.mpp-x")) {
                    mppX = jsonObject.get("openslide.mpp-x").toString();
                }
                if (jsonObject.containsKey("openslide.mpp-y")) {
                    mppY = jsonObject.get("openslide.mpp-y").toString();
                }
                if (jsonObject.containsKey("openslide.objective-power")) {
                    sourceLens = Integer.valueOf(jsonObject.get("openslide.objective-power").toString());
                }
            }
        }

        image.setResolutionX(mppX);
        image.setResolutionY(mppY);
        image.setSourceLens(sourceLens);
    }


    /**
     * 解析缩略图 把缩略图实际存储到本地
     *
     * @param inFile
     * @param id
     * @return
     */
    public void processThumbUpdate(File inFile, Long id) {
        Image image = imageMapper.selectById(id);
        image = processThumbInstance(inFile, image);
        if (ImageConstant.IMAGE_STATUS_PARSE_FAIL.equals(image.getStatus())) {
            processThumbInstance(inFile, image);
        }
        imageService.updateById(image);
    }

    private Image processThumbInstance(File inFile, Image image) {
        OpenSlide os = null;
        try {
            // 图片转换格式
            String srcPath = image.getImageUrl();
            String destPath = srcPath;
            ImageConversionsionResp resp = ImageUtils.pictureConversion(srcPath, destPath);
            destPath = resp.getDestPath();
            os = resp.getOpenSlide();
            if (os == null) {
                image.setImagePath(destPath);
                File file = new File(destPath);
                os = new OpenSlide(file);
            }
            // 推算文件绝对物理路径
            String thumbPath = image.getThumbUrl().replace(ImageConstant.THUMB_BASE_DIR, localFilePath);
            String cachePath = image.getCacheUrl();
            String labelPath = image.getLabelUrl().replace(ImageConstant.THUMB_BASE_DIR, localFilePath);
            String marcoPath = image.getMacroUrl().replace(ImageConstant.THUMB_BASE_DIR, localFilePath);

            // 检查文件夹，无则创建
            ImageUtils.checkDirectory(thumbPath);
            ImageUtils.checkDirectory(cachePath);
            ImageUtils.checkDirectory(labelPath);
            ImageUtils.checkDirectory(marcoPath);

            // 生成缩略图
            createThumbnailImage(os, thumbPath, imgSize);
            createThumbnailImage(os, cachePath, 1024);

            // 把缩略图、整个图片的长和宽存入image
            image = updateThumbWidthHeightTileCountImage(os, image, inFile.getAbsolutePath());
            if (image.getFormat().equals(ImageConstant.SVS) || image.getFormat().equals(ImageConstant.NDPI)) {
                writeRseolution(os, image);
            }

            // 总层数小于2为不可用 不可用原因共三种，2解析失败（不能获得缩略图）
            if (image.getLevelCount() < MIN_LEVEL_COUNT) {
                image.setStatus(ImageConstant.IMAGE_STATUS_PARSE_FAIL);
            } else {
//                image.setStatus(ImageConstant.IMAGE_STATUS_ENABLE);
            }
        } catch (Exception e) {
            log.error("1==>OpenSlideServiceImpl->processThumb->文件打开失败，转换后openSlide仍不识别此格式", e.getMessage());
            image.setStatus(ImageConstant.IMAGE_STATUS_PARSE_FAIL);
        } finally {
            if (os != null) {
                os.close();
                log.info("原始切片openslide对象已关闭，原始切片信息：[{}]", image);
            }
        }
        return image;
    }

    /**
     * 把缩略图、整个图片的长和宽存入image
     *
     * @param os
     * @param image
     * @param filePath
     * @return
     */
    private Image updateThumbWidthHeightTileCountImage(OpenSlide os, Image image, String filePath) {
        int levelCount = os.getLevelCount();
        // 更新levelCount
        image.setLevelCount(levelCount);
        String tileCountList = "";
        // 遍历存原生的每层切片个数
        for (int k = 0; k < levelCount; k++) {
            long l = os.getLevel0Width() / os.getLevelWidth(k) * os.getLevel0Height() / os.getLevelHeight(k);
            tileCountList += l;
            if (k != levelCount - 1) {
                tileCountList += ',';
            }
        }
        image.setTileCountList(tileCountList);
        image.setWidth(String.valueOf(os.getLevel0Width()));
        image.setHeight(String.valueOf(os.getLevel0Height()));

        double multiple = os.getLevel0Width() > os.getLevel0Height() ? 1024.0 / os.getLevel0Width() : 1024.0 / os.getLevel0Height();
        image.setMultiple(String.valueOf(multiple));
        return image;
    }

    /**
     * 生成本地缩略图
     *
     * @param os
     * @param path
     * @param size
     * @throws IOException
     */
    private void createThumbnailImage(OpenSlide os, String path, Integer size) throws IOException {
        log.info("开始生成缩略图：size: {}, path: {}", size, path);
        // 开始生成缩略图
        BufferedImage th = os.createThumbnailImage(size);
        String folderPath = new File(path).getParent();
        log.info("folderPath : " + folderPath);

        String resultName = folderPath + "/0." + imgType;
        File file = new File(folderPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        // 缩略图实际保存到本地
        ImageIO.write(th, imgType, new File(resultName));
    }


    @Override
    public void reparse(List<Long> imageIds) throws Exception {
        if (CollectionUtils.isEmpty(imageIds)) {
            throw new Exception("imageIds不能为空");
        }
        String[] status = new String[]{ImageConstant.IMAGE_STATUS_TILE_PROCESS_FAIL, ImageConstant.IMAGE_STATUS_PARSE_FAIL, ImageConstant.IMAGE_STATUS_MSG_PARSE_FAIL};
        List<Image> images = imageService.list(Wrappers.<Image>lambdaQuery().in(Image::getStatus, status)
                .eq(Image::getStatus, ImageConstant.IMAGE_STATUS_PARSE_FAIL).in(Image::getImageId, imageIds));
        processThumb(images);
    }

    /**
     * 创建缩略图
     *
     * @param images
     * @throws Exception
     */
    @Override
    public void processThumb(List<Image> images) throws Exception {
        if (CollectionUtils.isNotEmpty(images)) {
            CompletableFuture<?>[] futures = new CompletableFuture[images.size()];
            for (int i = 0; i < images.size(); i++) {
                Image image = images.get(i);
                try {
                    futures[i] = CompletableFuture.runAsync(() -> {
                        if (image.getStatus().equals(ImageConstant.IMAGE_STATUS_MSG_PARSE_FAIL)) {
                            // 移动文件到失败目录
                            moveFile2Failed(image);
                            log.warn("切片信息解析失败，不在执行下游流程, image: {}", image);
                            return;
                        }
                        image.setStatus(ImageConstant.IMAGE_STATUS_PARSING);
                        imageMapper.updateById(image);
                        processThumbInstance(new File(image.getImagePath()), image);
                        if (image.getStatus().equals(ImageConstant.IMAGE_STATUS_PARSE_FAIL)) {
                            imageMapper.updateById(image);
                            // 移动文件到失败目录
                            moveFile2Failed(image);
                            log.warn("切片缩略图解析失败，不在执行下游流程, image: {}", image);
                            return;
                        }
                        image.setStatus(ImageConstant.IMAGE_STATUS_TILE_PROCESSING);
                        imageMapper.updateById(image);

                        // 缩略图生成完成，提交切片任务到队列中异步处理
                        submitTileTask(image);
                    }, THREAD_POOL_EXECUTOR);
                }catch (Exception e) {
                    log.error("处理缩略图失败，image: {}, 异常信息: {}", image, e.getMessage());
                    image.setStatus(ImageConstant.IMAGE_STATUS_PARSE_FAIL);
                    moveFile2Failed(image);
                    imageMapper.updateById(image);
                    continue;
                }
            }
            // 等待所有任务完成
            CompletableFuture.allOf(futures).join();
        }
    }

    /**
     * 提交切片任务到异步处理队列
     * @param image
     */
    private void submitTileTask(Image image) {
        CompletableFuture.runAsync(() -> {
            try {
                String out_path = localFilePath + File.separator + ImageUtils.getOrgIdFormat(image.getOrganizationId()) + File.separator + image.getImageId() + File.separator + "TileGroup0";
                log.info("开始调用python脚本处理切片：image: {}, 切片输出路径：{}", image, out_path);
                callPython(image.getImagePath(), out_path);

                // 更新状态为启用
                image.setStatus(ImageConstant.IMAGE_STATUS_ENABLE);
                image.setUpdateTime(new Date());
                imageMapper.updateById(image);
            } catch (Exception e) {
                image.setStatus(ImageConstant.IMAGE_STATUS_TILE_PROCESS_FAIL);
                moveFile2Failed(image);
                image.setUpdateTime(new Date());
                imageMapper.updateById(image);
                log.error("调用python脚本失败，image：{}，异常信息：{}", image, e.getMessage());
            }
        }, FIXED_THREAD_POOL_EXECUTOR);
    }

    /**
     * 单独处理切片任务（解耦后的切片处理方法）
     * @param images
     * @throws Exception
     */
    @Override
    public void processTiles(List<Image> images) throws Exception {
        if (CollectionUtils.isNotEmpty(images)) {
            CompletableFuture<?>[] futures = new CompletableFuture[images.size()];
            for (int i = 0; i < images.size(); i++) {
                Image image = images.get(i);
                futures[i] = CompletableFuture.runAsync(() -> {
                    if (!image.getStatus().equals(ImageConstant.IMAGE_STATUS_TILE_PROCESSING)) {
                        log.warn("图像状态不正确，无法处理切片任务, image: {}, status: {}", image, image.getStatus());
                        return;
                    }

                    try {
                        String out_path = localFilePath + File.separator + ImageUtils.getOrgIdFormat(image.getOrganizationId()) + File.separator + image.getImageId() + File.separator + "TileGroup0";
                        log.info("开始调用python脚本处理切片：image: {}, 切片输出路径：{}", image, out_path);
                        callPython(image.getImagePath(), out_path);

                        image.setStatus(ImageConstant.IMAGE_STATUS_ENABLE);
                    } catch (Exception e) {
                        image.setStatus(ImageConstant.IMAGE_STATUS_TILE_PROCESS_FAIL);
                        moveFile2Failed(image);
                        log.error("调用python脚本失败，image：{}，异常信息：{}", image, e.getMessage());
                    } finally {
                        image.setUpdateTime(new Date());
                        imageMapper.updateById(image);
                    }
                }, FIXED_THREAD_POOL_EXECUTOR);
            }
            // 等待所有任务完成
            CompletableFuture.allOf(futures).join();
        }
    }

    /**
     * 创建缩略图
     *
     * @param image
     * @throws Exception
     */
    @Override
    public void processThumb(Image image) throws Exception {
        if (ObjectUtil.isNotEmpty(image)) {
            List<Image> images = Collections.singletonList(image);
            processThumb(images);
        }
    }

    /**
     * 移动文件到失败目录
     * @param image
     */
    private void moveFile2Failed(Image image) {
        Path sourcePath = Paths.get(image.getImageUrl());
        if (Files.exists(sourcePath)) {
            try {
                String failedDir = localFilePath + File.separator +
                        ImageUtils.getFourNumber(image.getOrganizationId()) +
                        File.separator + "Failed";

                Path failedFolderPath = Paths.get(failedDir);
                if (!Files.exists(failedFolderPath)) {
                    Files.createDirectories(failedFolderPath);
                }

                // 检查目录是否可写
                if (!Files.isWritable(failedFolderPath)) {
                    log.error("失败目录不可写: {}", failedDir);
                    return;
                }

                Path targetPath = failedFolderPath.resolve(sourcePath.getFileName());

                // 使用 Files.move 进行移动
                try {
                    Files.move(sourcePath, targetPath,
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                    log.info("文件移动成功: {} -> {}", sourcePath, targetPath);
                } catch (AtomicMoveNotSupportedException e) {
                    // 如果原子移动不支持，尝试普通移动
                    log.warn("原子移动不支持，使用普通移动: {}", e.getMessage());
                    Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    log.info("文件移动成功: {} -> {}", sourcePath, targetPath);
                }

            } catch (Exception e) {
                log.error("移动文件到失败目录时发生异常，imageId: {}", image.getImageId(), e);

                // 如果移动失败，尝试复制后删除
                try {
                    String failedDir = localFilePath + File.separator +
                            ImageUtils.getFourNumber(image.getOrganizationId()) +
                            File.separator + "Failed";
                    Path failedFolderPath = Paths.get(failedDir);
                    Path targetPath = failedFolderPath.resolve(sourcePath.getFileName());

                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    Files.delete(sourcePath);
                    log.info("文件复制+删除成功: {} -> {}", sourcePath, targetPath);
                } catch (IOException copyException) {
                    log.error("复制文件也失败了，imageId: {}", image.getImageId(), copyException);
                }
            }
        } else {
            log.warn("源文件不存在，无法移动: {}", image.getImageUrl());
        }
    }


    public void callPython(String imagePath, String tileDir) throws Exception {
        // Specify the Python script and its arguments
        ProcessBuilder processBuilder = new ProcessBuilder(pythonExecutable, pythonScriptPath, imagePath, tileDir);
        // Redirect error stream to output stream
        processBuilder.redirectErrorStream(true);
        // Start the process
        Process process = processBuilder.start();
        // Read the output from the process
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
        // Wait for the process to complete
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.error("Python script executed failed: exit code=[{}] imagePath={}, tileDir={}", exitCode, imagePath, tileDir);
            throw new Exception("Python script failed with exit code: " + exitCode);
        }
        log.info("Python script executed successfully: exit code=[{}] imagePath={}, tileDir={}", exitCode, imagePath, tileDir);
    }

}
