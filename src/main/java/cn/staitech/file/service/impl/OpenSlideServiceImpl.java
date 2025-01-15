package cn.staitech.file.service.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import cn.staitech.common.core.utils.uuid.IdUtils;
import cn.staitech.common.security.utils.SecurityUtils;
import cn.staitech.file.constant.ImageConstant;
import cn.staitech.file.domain.Image;
import cn.staitech.file.domain.Topic;
import cn.staitech.file.mapper.ImageMapper;
import cn.staitech.file.service.FrService;
import cn.staitech.file.service.ImageService;
import cn.staitech.file.service.OpenSlideService;
import cn.staitech.file.util.FileUploadUtils;
import cn.staitech.file.util.ImageConversionsionResp;
import cn.staitech.file.util.ImageUtils;
import cn.staitech.file.vo.FileInsertVO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.openslide.AssociatedImage;
import org.openslide.OpenSlide;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * OpenSlideServiceImpl
 */
@Slf4j
@Service
public class OpenSlideServiceImpl implements OpenSlideService {

    private static Snowflake snowflake = IdUtil.getSnowflake();

    //@Value("${open-slide.img-type}")
    private final String imgType = "jpg";

    //@Value("${open-slide.img-size}")
    private final Integer imgSize = 256;

    /**
     * 智能阅片check
     */
    @Value("${frinspection.check}")
    private boolean check;

    @Value("${file.path}")
    private String localFilePath;

    @Autowired
    private ImageService imageService;

    @Autowired
    private ImageMapper imageMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private FrService frService;

    @Resource
    private Cache<String, Object> cache;
    //private final static Map<String, FileInsertVO> imageMap = new ConcurrentHashMap<>();

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
    public void writeRseolution(OpenSlide os, Image image) {
        String mppX = "";
        String mppY = "";
        Integer sourceLens = 0;

        Map<String, String> properties = os.getProperties();
        // log.info("writeRseolution --> image id: {} path: {} properties: {}", image.getImageId(), image.getImagePath(), properties);

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

    /**
     * 20231109需求：解析失败后重新解析一次
     *
     * @param inFile
     * @param image
     * @return
     */
    public void processThumbSave(File inFile, Image image) {
        image = processThumbInstance(inFile, image);
        if (ImageConstant.IMAGE_PROCESS_PARSE_FAIL.equals(image.getProcessFlag())) {
            processThumbInstance(inFile, image);
        }
        imageService.saveOrUpdate(image);
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
                log.info("****************OpenSlide验证未通过,转换后图像地址:[{}]***************", destPath);
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

            Map<String, AssociatedImage> map = os.getAssociatedImages();

            /*if (map.containsKey("label")) {
                createImage(map, labelPath, "label");
            }
            if (map.containsKey("macro")) {
                createImage(map, marcoPath, "macro");
            }*/

            // 把缩略图、整个图片的长和宽存入image
            image = updateThumbWidthHeightTileCountImage(os, image, inFile.getAbsolutePath());
            if (image.getFormat().equals(ImageConstant.SVS) || image.getFormat().equals(ImageConstant.NDPI)) {
                writeRseolution(os, image);
            }
            imageService.saveOrUpdate(image);
            // 总层数小于2为不可用 不可用原因共三种，2解析失败（不能获得缩略图）
            if (image.getLevelCount() < MIN_LEVEL_COUNT) {
                image.setProcessFlag(ImageConstant.IMAGE_PROCESS_PARSE_FAIL);
                image.setStatus(ImageConstant.IMAGE_STATUS_UNABLE);
            } else {
                //TODO 是否需要算法清晰度校验    type:1 原始切片 2：预测切片
                if (check && image.getFormat().equals(ImageConstant.SVS) && image.getBizType()==1) {
                    log.info("算法校验开始,ImageId:[{}],image:[{}]", image.getImageId(),image);
                    //通知算法校验
                    frService.verification(image);
                }else{
                    // 可用
                    image.setStatus(ImageConstant.IMAGE_STATUS_ENABLE);
                }
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

    /**
     * 生成 label、macro
     *
     * @param map
     * @param path
     * @param type
     * @throws IOException
     */
    private void createImage(Map<String, AssociatedImage> map, String path, String type) throws IOException {
        String folderPath = new File(path).getParent();
        String resultName = folderPath + "/0." + imgType;
        File file = new File(folderPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        AssociatedImage associatedImage = map.get(type);
        BufferedImage ar = associatedImage.toBufferedImage();
        BufferedImage result = new BufferedImage(ar.getWidth(), ar.getHeight(), 1);
        Graphics2D g = result.createGraphics();
        g.drawImage(ar, 0, 0, null);
        ImageIO.write(result, imgType, new File(resultName));
    }

    /**
     * 重新解析原始切片缩略图
     *
     * @param imageIds
     * @throws Exception
     */
    public void reparse(List<Long> imageIds) throws Exception {
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(processors * 2 + 1, processors * 4, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100000));
        if (imageIds.isEmpty()) {
            return;
        }
        QueryWrapper<Image> queryWrapper = Wrappers.query();
        queryWrapper.eq("status", ImageConstant.IMAGE_STATUS_UNABLE);
        queryWrapper.eq("process_flag", ImageConstant.IMAGE_PROCESS_PARSE_FAIL);
        queryWrapper.in("image_id", imageIds);
        List<Image> list = imageService.list(queryWrapper);
        if (list != null && !list.isEmpty()) {
            CountDownLatch countDownLatch = new CountDownLatch(list.size());
            for (Image image : list) {
                threadPoolExecutor.submit(new ReparseImageTask(countDownLatch, image));
            }
            countDownLatch.await();
        }
        threadPoolExecutor.shutdown();
    }

    @Override
    public Cache<String, Object> getCache() throws Exception {
        return cache;
    }

    class ReparseImageTask implements Runnable {
        private CountDownLatch countDownLatch;
        private Image image;

        public ReparseImageTask(CountDownLatch countDownLatch, Image image) {
            this.countDownLatch = countDownLatch;
            this.image = image;
        }

        @Override
        public void run() {
            try {
                log.info("开始重新解析原始切片缩略图,原始切片信息:[{}]", image);
                processThumbUpdate(new File(image.getImagePath()), image.getImageId());
                log.info("重新解析原始切片缩略图完成,原始切片信息:[{}]", image);
            } catch (Exception e) {
                log.error("重新解析原始切片缩略图异常：[{}],原始切片信息:[{}]", e.getMessage(), image);
            } finally {
                countDownLatch.countDown();
            }
        }
    }

    /**
     * 异步批量服务器读取切片
     *
     * @param vo
     * @throws Exception
     */
    @Override
    public void asynSaveBatch(FileInsertVO vo) {
        if (cache.getIfPresent(vo.getTopicName()) == null) {
            cache.put(vo.getTopicName(), vo);
            int processors = Runtime.getRuntime().availableProcessors();
            ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(processors * 2 + 1, processors * 4, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100000));
            String[] paths = vo.getFileList();
            if (paths.length > 0) {
                try {
                    Topic topic = imageService.getTopic(vo.getTopicName(), vo.getBizType());
                    CountDownLatch countDownLatch = new CountDownLatch(paths.length);
                    for (String path : paths) {
                        threadPoolExecutor.submit(new ImageTask(countDownLatch, path, topic));
                    }
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    log.error("服务器读取切片异常：[{}]", e.getMessage());
                } finally {
                    cache.invalidate(vo.getTopicName());
                    threadPoolExecutor.shutdown();
                }
            }
        }
    }

    class ImageTask implements Runnable {

        private CountDownLatch countDownLatch;
        private String path;
        private Topic topic;

        public ImageTask(CountDownLatch countDownLatch, String path, Topic topic) {
            this.countDownLatch = countDownLatch;
            this.path = path;
            this.topic = topic;
        }

        @Override
        public void run() {
            try {
                Long loginUser = SecurityUtils.getUserId();
                File file = new File(path);
                String imageName = file.getName();
                Integer count = imageMapper.selectCount(Wrappers.<Image>lambdaQuery().eq(Image::getImageName, imageName).eq(Image::getTopicName, topic.getTopicName()));
                if (count > 0) {
                    log.warn("服务器选片异常:[{}]该文件已经存在,topicName:[{}]", imageName,topic.getTopicName());
                    countDownLatch.countDown();
                    return;
                }
                /*if (Long.valueOf(file.length()) > ImageConstant.ALLOWED_FILE_MAXSIZE) {
                    log.warn("服务器选片异常:[{}]该文件大小超过5g", imageName);
                    countDownLatch.countDown();
                    return;
                }*/
                Image image = new Image();
                image.setBizType(topic.getProjectTypeId());
                image.setOrganizationId(topic.getOrganizationId());
                image.setImagePath(path);
                image.setImageUrl(path);
                image.setImageName(imageName);
                image.setSize(String.valueOf(file.length()));
                // 去掉文件扩展名的文件名称
                image.setFileName(FileUploadUtils.getFileName(file.getName()));
                image.setCreateBy(loginUser);
                image.setUpdateBy(loginUser);
                image.setStatus(ImageConstant.IMAGE_STATUS_UNABLE);
                image.setProcessFlag(ImageConstant.IMAGE_PROCESS_PARSING);
                image.setImageCode(IdUtils.randomUUID());
                image.setDelFlag(ImageConstant.NOT_DELETED);
                image.setTopicId(topic.getTopicId());
                image.setTopicName(topic.getTopicName());
                image.setSource(ImageConstant.IMAGE_SOURCE_SERVER);
                image.setFormat(image.getImageName().substring(image.getImageName().lastIndexOf('.') + 1));
                // 插入数据 - 生成文件目录、文件名 start
                String folderName = DateUtil.format(new Date(), DatePattern.PURE_DATE_PATTERN);
                String filePathStr = folderName + "/" + snowflake.nextIdStr() + "/0.jpg";
                String thumbPath = ImageConstant.THUMB_BASE_DIR + File.separator + ImageUtils.getFourNumber(image.getOrganizationId()) + "/thumbnail/" + filePathStr;
                String macroPath = ImageConstant.THUMB_BASE_DIR + File.separator + ImageUtils.getFourNumber(image.getOrganizationId()) + "/macro/" + filePathStr;
                String labelPath = ImageConstant.THUMB_BASE_DIR + File.separator + ImageUtils.getFourNumber(image.getOrganizationId()) + "/label/" + filePathStr;
                String cacheURL = localFilePath+ ImageUtils.getFourNumber(image.getOrganizationId()) + "/cacheThumbnail/" + filePathStr;
                if (image.getBizType() == 2) {
                    cacheURL = "/home/pat_saas/" + ImageUtils.getFourNumberNoSlide(image.getOrganizationId()) + "/Upload/" + image.getTopicName() + "/";
                }
                image.setThumbUrl(thumbPath);
                image.setMacroUrl(macroPath);
                image.setLabelUrl(labelPath);
                image.setCacheUrl(cacheURL);
                // 生成缩略图
                processThumbSave(file, image);
            } catch (Exception e) {
                log.error("服务器选片异常:[{}],原始切片地址：[{}]", e.getMessage(), path);
            } finally {
                countDownLatch.countDown();
            }
        }
    }

}
