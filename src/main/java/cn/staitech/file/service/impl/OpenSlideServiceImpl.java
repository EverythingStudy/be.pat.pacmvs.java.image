package cn.staitech.file.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.staitech.file.constant.ImageConstant;
import cn.staitech.file.domain.Image;
import cn.staitech.file.mapper.ImageMapper;
import cn.staitech.file.service.ImageService;
import cn.staitech.file.service.OpenSlideService;
import cn.staitech.file.util.FileUploadUtils;
import cn.staitech.file.util.ImageConversionsionResp;
import cn.staitech.file.util.ImageUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.openslide.OpenSlide;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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

    private static Snowflake snowflake = IdUtil.getSnowflake();

    private final String imgType = "jpg";

    private final Integer imgSize = 256;

    @Value("${file.path}")
    private String localFilePath;

    @Resource
    private ImageService imageService;

    @Resource
    private ImageMapper imageMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final ThreadPoolExecutor THREAD_POOL_EXECUTOR;

    static {
        int processors = Runtime.getRuntime().availableProcessors();
        THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
                processors * 2 + 1,
                processors * 4,
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
                new RejectedExecutionHandler(){
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        // 丢弃任务，不抛出异常
                        log.error("OpenSlide THREAD_POOL_EXECUTOR rejectedExecution: {}", r);
                    }
                }
        );
    }

    /**
     * 总层数小于2为不可用
     */
    private final int MIN_LEVEL_COUNT = 2;

    /**
     * 小文件上传
     *
     * @param file    上传的文件
     * @param imageId 图像ID
     * @return 访问地址
     * @throws Exception
     */
    @Transactional
    public String uploadFile(MultipartFile file, Long imageId) throws Exception {
        Image image = imageMapper.selectById(imageId);
        String imagePath = FileUploadUtils.upload(image, file);
        image.setImagePath(imagePath);
        // 文件md5值校验
        try {
            FileInputStream inputStream = new FileInputStream(ResourceUtils.getFile(imagePath));
            String tMd5 = DigestUtils.md5DigestAsHex(inputStream);
            // 文件上传成功,合并成功 0上传失败（MD5校验不通过），1解析中，2解析失败（不能获得缩略图）
            if (tMd5.equals(image.getMd5())) {
                image.setProcessFlag(ImageConstant.IMAGE_PROCESS_PARSE_SUCCESS);
                image.setStatus(ImageConstant.IMAGE_STATUS_ENABLE);
                log.info("文件md5值校验成功,imageId:{} ,MD5:{}", imageId, tMd5);
            } else {
                image.setProcessFlag(ImageConstant.IMAGE_PROCESS_PARSE_FAIL);
                image.setStatus(ImageConstant.IMAGE_STATUS_UNABLE);
                log.info("文件md5值校验失败-2,imageId:{} ,MD5:{}", imageId, tMd5);
            }

            int update = imageMapper.updateById(image);

            String cacheKey = ImageUtils.getPathKey(String.valueOf(imageId));

            if (update > 0) {
                log.info("小文件上传成功 imageid:{} ,imagePath:{}", imageId, imagePath);
                // 存入redis
                stringRedisTemplate.opsForValue().set(cacheKey, imagePath);
                return imagePath;
            } else {
                // 删除 redis
                log.info("小文件上传失败 imageid:{} ,imagePath:{}", imageId, imagePath);
                stringRedisTemplate.delete(cacheKey);
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("小图像上传异常：{};;imageId:{};;;fileName:{}", e.getMessage(), imageId, file.getName());
            throw e;
        }
    }

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
        if (ImageConstant.IMAGE_PROCESS_PARSE_FAIL.equals(image.getProcessFlag())) {
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
            ImageConversionsionResp resp = FileUploadUtils.pictureConversion(srcPath, destPath);
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
            FileUploadUtils.checkDirectory(thumbPath);
            FileUploadUtils.checkDirectory(cachePath);
            FileUploadUtils.checkDirectory(labelPath);
            FileUploadUtils.checkDirectory(marcoPath);

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
                image.setProcessFlag(ImageConstant.IMAGE_PROCESS_PARSE_FAIL);
                image.setStatus(ImageConstant.IMAGE_STATUS_UNABLE);
            } else {
                image.setStatus(ImageConstant.IMAGE_STATUS_ENABLE);
                image.setProcessFlag(ImageConstant.IMAGE_PROCESS_PARSE_SUCCESS);
            }
        } catch (Exception e) {
            log.error("1==>OpenSlideServiceImpl->processThumb->文件打开失败，转换后openSlide仍不识别此格式", e.getMessage());
            image.setProcessFlag(ImageConstant.IMAGE_PROCESS_PARSE_FAIL);
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
        List<Image> images = imageService.list(Wrappers.<Image>lambdaQuery().eq(Image::getStatus, ImageConstant.IMAGE_STATUS_UNABLE)
                .eq(Image::getProcessFlag, ImageConstant.IMAGE_PROCESS_PARSE_FAIL).in(Image::getImageId, imageIds));
        processThumb(images);
    }

    /**
     * 创建缩略图
     * @param images
     * @throws Exception
     */
    @Override
    public void processThumb(List<Image> images) throws Exception {
        if (CollectionUtils.isNotEmpty(images)) {
            CompletableFuture<?>[] futures = new CompletableFuture[images.size()];
            for (int i = 0; i < images.size(); i++) {
                Image image = images.get(i);
                futures[i] = CompletableFuture.runAsync(() -> {
                    processThumbInstance(new File(image.getImagePath()), image);
                    imageMapper.updateById(image);
                }, THREAD_POOL_EXECUTOR);
            }
        }
    }

    @Override
    public void processThumb(Image image) throws Exception {
        if (ObjectUtil.isNotEmpty(image)){
            List<Image> images = Arrays.asList(image);
            processThumb(images);
        }
    }

}
