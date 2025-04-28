package cn.staitech.file.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import cn.staitech.common.core.domain.R;
import cn.staitech.common.core.utils.uuid.IdUtils;
import cn.staitech.common.security.utils.SecurityUtils;
import cn.staitech.file.constant.ImageConstant;
import cn.staitech.file.domain.Image;
import cn.staitech.file.util.FileUploadUtils;
import cn.staitech.file.domain.Topic;
import cn.staitech.file.mapper.ImageMapper;
import cn.staitech.file.mapper.TopicMapper;
import cn.staitech.file.service.ImageService;
import cn.staitech.file.util.ImageUtils;
import cn.staitech.file.vo.FileInformationOutVO;
import cn.staitech.file.vo.FileInformationVO;
import cn.staitech.file.vo.FileInsertVO;
import cn.staitech.system.api.domain.SysUser;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2025/4/22 09:32:40
 */
@Slf4j
@Service
@Transactional
public class ImageServiceImpl extends ServiceImpl<ImageMapper, Image> implements ImageService {

    @Value("${file.path}")
    private String localFilePath;
    @Resource
    private ImageMapper imageMapper;
    @Resource
    private TopicMapper topicMapper;
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static Snowflake snowflake = IdUtil.getSnowflake();

    // 项目启动时，初始化imagePath到缓存
    @PostConstruct
    public void init() {
        loadingDictCache();
    }

    public void loadingDictCache() {
        Set<String> keys = redisTemplate.keys(ImageUtils.PATH_IMAGE_KEY_ALL);
        redisTemplate.delete(keys);
        List<Image> images = imageMapper.selectList(Wrappers.query());
        // 遍历image表，把所有行的imagePath存入redis
        for (Image image : images) {
            if (StringUtils.isNotBlank(image.getImagePath())) {
                String cacheKey = ImageUtils.getPathKey(String.valueOf(image.getImageId()));
                stringRedisTemplate.opsForValue().set(cacheKey, image.getImagePath());
            }
        }
    }


    @Override
    public List<Image> batchInsert(FileInsertVO vo) throws Exception {

        if (vo.getFileList() == null || vo.getFileList().length == 0) {
            throw new Exception("图片文件绝对路径数组不可为空");
        }
        List<Image> existImages = imageMapper.selectList(Wrappers.<Image>lambdaQuery().in(Image::getImagePath, vo.getFileList()).eq(Image::getOrganizationId, vo.getOrganizationId()));
        if (CollectionUtils.isNotEmpty(existImages)) {
            throw new DuplicateKeyException("服务器选片异常:[" + existImages.stream().map(Image::getImagePath).collect(Collectors.toList()) + "]文件已经存在");
        }
        List<Image> images = new ArrayList<>();
        for (String path : vo.getFileList()) {
            Long loginUser = SecurityUtils.getUserId();
            File file = new File(path);
            String imageName = file.getName();
            Image image = new Image();
            image.setBizType(vo.getBizType());
            image.setOrganizationId(vo.getOrganizationId());
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
            image.setDelFlag("1");
            image.setSource(ImageConstant.IMAGE_SOURCE_SERVER);
            image.setFormat(image.getImageName().substring(image.getImageName().lastIndexOf('.') + 1));
            // 插入数据 - 生成文件目录、文件名 start
            String folderName = DateUtil.format(new Date(), DatePattern.PURE_DATE_PATTERN);
            String filePathStr = folderName + "/" + snowflake.nextIdStr() + "/0.jpg";
            String thumbPath = ImageConstant.THUMB_BASE_DIR + File.separator + ImageUtils.getFourNumber(image.getOrganizationId()) + File.separator + "thumbnail" + File.separator + filePathStr;
            String macroPath = ImageConstant.THUMB_BASE_DIR + File.separator + ImageUtils.getFourNumber(image.getOrganizationId()) + File.separator + "macro" + File.separator + filePathStr;
            String labelPath = ImageConstant.THUMB_BASE_DIR + File.separator + ImageUtils.getFourNumber(image.getOrganizationId()) + File.separator + "label" + File.separator + filePathStr;
            String cacheURL = localFilePath + File.separator + ImageUtils.getFourNumber(image.getOrganizationId()) + File.separator + "cacheThumbnail" + File.separator + filePathStr;
            image.setThumbUrl(thumbPath);
            image.setMacroUrl(macroPath);
            image.setLabelUrl(labelPath);
            image.setCacheUrl(cacheURL);
            parseFields(imageName, image);
            images.add(image);
        }
        saveBatch(images);
        return images;
    }

    /**
     * 上传文件前置信息,向数据库中增加一条图像信息,初始化,存入MD5等信息
     *
     * @param fileInformation
     * @return
     * @throws Exception
     */
    @Override
    @Transactional
    public R<FileInformationOutVO> insert(FileInformationVO fileInformation) throws Exception {
        // 项目类型
        Integer bizType = fileInformation.getProjectTypeId() > Integer.valueOf(0) ? fileInformation.getProjectTypeId() : Integer.valueOf(1);
        // 项目类型(为topic类型特殊处理)
        // 向MySQL插入数据 - 生成文件目录、文件名等
        // 1、 the first insert into image table get imageId
        // 2、 Produce image`s paths  ->savePath()
        // 3、 Update image table
        // 4、Add Image to Redis
        Long userId = SecurityUtils.getUserId();
        Image image = new Image();
        // 浅拷贝，把req里面的值拷贝到image中，利用封装的SpringUtils，进行只有非null值覆盖
        BeanUtil.copyProperties(fileInformation, image);

        try {
            image.setBizType(bizType);
            image.setOrganizationId(fileInformation.getOrganizationId());
            image.setStatus(ImageConstant.IMAGE_STATUS_UNABLE);
            //image.setProcessFlag(ImageConstant.IMAGE_PROCESS_PARSING);
            image.setImageCode(IdUtils.randomUUID());
            image.setUpdateBy(userId);
            image.setCreateBy(userId);
            image.setSource(ImageConstant.IMAGE_SOURCE_UPLOAD);
            // 逻辑删除状态（0删除，1未删除）
            image.setDelFlag("1");
            // 去掉文件扩展名的文件名称
            String fileName = FileUploadUtils.getFileName(fileInformation.getImageName());
            image.setFileName(fileName);
            // 拆分图片名称字段
            parseFields(fileInformation.getImageName(), image);
            int insert = imageMapper.insert(image);
            if (insert > 0) {
                FileInformationOutVO out = new FileInformationOutVO();
                out.setImageId(image.getImageId());
                {
                    // 初始化文件路径
                    savePath(image);
                    // 修改MySQL中图像信息
                    imageMapper.updateById(image);
                    // 存入redis
                    String cacheKey = ImageUtils.getPathKey(String.valueOf(image.getImageId()));
                    stringRedisTemplate.opsForValue().set(cacheKey, image.getImagePath());
                }
                return R.ok(out, "补充信息成功");
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("FileInformation信息创建异常：{};;{}", e.getMessage(), fileInformation);
            throw e;
        }
        return R.fail("补充信息失败");
    }

    /**
     * 拆分图片名称
     *
     * @param input 输入字符串
     */
    private Image parseFields(String input, Image image) {
        // 根据空格拆分字符串为三个主要部分
        String[] parts = input.split(" ");
        // 文件名解析状态:默认1成功
        image.setAnalyzeStatus(ImageConstant.NUMBER_1);
        try {
            if (parts.length >= 3) {
                // 解析专题号部分
                String topicNumber = parts[0].trim();
                image.setTopicName(topicNumber);
                Topic topic = getTopic(topicNumber);
                if (topic != null) {
                    image.setTopicId(topic.getTopicId());
                } else {
                    log.error("未找到专题信息，专题号: {}", topicNumber);
                    image.setAnalyzeStatus(ImageConstant.NUMBER_0);
                }

                // 解析动物号和蜡块号部分
                String animalAndWaxBlock = parts[1].trim();
                int lastIndex = animalAndWaxBlock.lastIndexOf('-');
                if (lastIndex == -1 || lastIndex == animalAndWaxBlock.length() - 1) {
                    log.error("动物号和蜡块号格式无效: {}", animalAndWaxBlock);
                    image.setAnalyzeStatus(ImageConstant.NUMBER_0);
                }
                String waxBlockStr = animalAndWaxBlock.substring(lastIndex + 1);
                image.setWaxCode(waxBlockStr);
                String animalNumber = animalAndWaxBlock.substring(0, lastIndex);
                image.setAnimalCode(animalNumber);

                // 解析组号和性别部分
                String groupNumberAndGender = parts[2].trim();
                if (groupNumberAndGender.length() >= 2) {
                    String groupNumber = groupNumberAndGender.substring(0, groupNumberAndGender.length() - 1);
                    String gender = groupNumberAndGender.substring(groupNumberAndGender.length() - 1);
                    image.setGroupCode(groupNumber);
                    image.setSexFlag(gender);
                } else {
                    log.error("组号和性别格式无效: {}", groupNumberAndGender);
                    image.setAnalyzeStatus(ImageConstant.NUMBER_0);
                }
            } else {
                parseSlideCode(input, image);
            }
        } catch (Exception e) {
            image.setAnalyzeStatus(ImageConstant.NUMBER_0);
            log.error("文件名:[{}]解析失败：[{}]", input, e.getMessage());
            if (log.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
        return image;
    }

    /**
     * 拆分图片名称字段
     *
     * @param input 输入字符串，格式为"专题号~动物号~蜡块号~组别性别~其他尾缀_时间戳"
     * @param image 图像对象，用于存储解析结果
     * @return 更新后的图像对象
     * @throws Exception 如果解析失败，抛出异常
     */
    private Image parseSlideCode(String input, Image image) throws Exception {
        // 根据波浪号拆分字符串为五个主要部分
        input = StringUtils.replace(input, " ", "");
        StringUtils.trimToEmpty(input);
        String[] parts = input.split("~");
        // 文件名解析状态: 默认1成功
        image.setAnalyzeStatus(ImageConstant.NUMBER_1);
        if (parts.length == 4 || parts.length == 5) {
            // 解析专题号部分
            String topicNumber = parts[0].trim();
            image.setTopicName(topicNumber);
            Topic topic = getTopic(topicNumber);
            if (topic != null) {
                image.setTopicId(topic.getTopicId());
            } else {
                log.error("切片编号：[{}], 未找到专题信息，专题号: {}", input, topicNumber);
                image.setAnalyzeStatus(ImageConstant.NUMBER_0);
            }
            // 解析动物号和蜡块号部分
            String animalCode = parts[1].trim();
            image.setAnimalCode(animalCode);
            String waxBlock = parts[2].trim();
            image.setWaxCode(waxBlock);
            // 解析组号和性别部分
            String groupNumberAndGender = parts[3].trim();
            if (groupNumberAndGender.contains("_")) {
                groupNumberAndGender = groupNumberAndGender.substring(0, groupNumberAndGender.indexOf("_"));
            }
            String groupNumberAndGenderEnd = StringUtils.substring(groupNumberAndGender, groupNumberAndGender.length() - 1);
            if (groupNumberAndGender.length() >= 2 && ("M".equals(groupNumberAndGenderEnd) || "F".equals(groupNumberAndGenderEnd))) {
                String groupNumber = groupNumberAndGender.substring(0, groupNumberAndGender.length() - 1);
                String gender = groupNumberAndGender.substring(groupNumberAndGender.length() - 1);
                image.setGroupCode(groupNumber);
                image.setSexFlag(gender);
            } else {
                log.error("切片编号：[{}], 组号和性别格式无效: {}", input, groupNumberAndGender);
                image.setAnalyzeStatus(ImageConstant.NUMBER_0);
            }

        } else {
            log.error("切片编号：[{}],输入格式无效", input);
            image.setAnalyzeStatus(ImageConstant.NUMBER_0);
        }
        return image;
    }

    /**
     * 文件前置信息上传-初始化文件路径
     *
     * @param image
     * @return
     */
    public Image savePath(Image image) {
        image.setFormat(image.getImageName().substring(image.getImageName().lastIndexOf('.') + 1));
        image.setCreateBy(SecurityUtils.getUserId());
        image.setCreateTime(new Date());

        // 插入数据 - 生成文件目录、文件名 start
        Long imageId = image.getImageId();
        //时间格式化格式
        Date currentTime = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String filePrefix = simpleDateFormat.format(currentTime);
        //拼接新的文件名
        simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        String folderName = simpleDateFormat.format(currentTime);
        String imagePathDir = localFilePath + File.separator + ImageUtils.getFourNumber(image.getOrganizationId()) +
                File.separator + image.getTopicName() + File.separator;
        /*String imagePath = imagePathDir + newFileName;
        String imageURL = imagePathDir + newFileName;*/
        String imagePath = imagePathDir + image.getImageName();
        String imageURL = imagePathDir + image.getImageName();
        image.setImagePath(imagePath);
        image.setImageUrl(imageURL);
        File imageDir = new File(imagePathDir);
        if (!imageDir.exists() && !imageDir.isDirectory()) {
            imageDir.mkdirs();
        }
        String filePathStr = folderName + File.separator + imageId + File.separator + "0.jpg";
        String thumbPath = ImageConstant.THUMB_BASE_DIR + File.separator + ImageUtils.getFourNumber(image.getOrganizationId()) + File.separator + "thumbnail" + File.separator + filePathStr;
        String macroPath = ImageConstant.THUMB_BASE_DIR + File.separator + ImageUtils.getFourNumber(image.getOrganizationId()) + File.separator + "macro" + File.separator + filePathStr;
        String labelPath = ImageConstant.THUMB_BASE_DIR + File.separator + ImageUtils.getFourNumber(image.getOrganizationId()) + File.separator + "label" + File.separator + filePathStr;
        String cacheURL = localFilePath + File.separator + ImageUtils.getFourNumber(image.getOrganizationId()) + File.separator + "cacheThumbnail" + File.separator + filePathStr;
        image.setThumbUrl(thumbPath);
        image.setMacroUrl(macroPath);
        image.setLabelUrl(labelPath);
        image.setCacheUrl(cacheURL);

        return image;
    }


    /**
     * 校验文件MD5及文件名是否存在
     *
     * @param in
     * @return
     */
    @Override
    public boolean checkImageNameAndMd5(FileInformationVO in) {
        /*List<Image> images = getBaseMapper().selectList(Wrappers.query(Image.builder().md5(in.getMd5()).imageName(in.getImageName())
                .status(ImageConstant.IMAGE_STATUS_ENABLE).delFlag(DataConstants.NOT_DELETED).build()));*/
        List<Image> images = getBaseMapper().selectList(Wrappers.query(Image.builder().imageName(in.getImageName())
                .delFlag("1").build()));
        if (images.isEmpty()) {
            return true;
        }
        return false;
    }


    /**
     * 处理专题数据
     *
     * @param topicName
     * @return
     */
    @Override
    public Topic getTopic(String topicName) {
        SysUser sysUser = SecurityUtils.getLoginUser().getSysUser();
        Long userId = sysUser.getUserId();

        Topic topic = Topic.builder()
                .topicName(topicName)
                .organizationId(sysUser.getOrganizationId())
                .build();

        QueryWrapper queryWrap = new QueryWrapper(topic);
        Topic qTopic = topicMapper.selectOne(queryWrap);

        // 有则返回
        if (qTopic != null) {
            return qTopic;
        } else { // 无则添加
            topic.setCreateBy(userId);
            topic.setUpdateBy(userId);
            topic.setCreateTime(new Date());
            topic.setUpdateTime(new Date());
            topic.setOrganizationId(sysUser.getOrganizationId());
            topic.setTopicName(topicName);
            try {
                topicMapper.insert(topic);
            } catch (DuplicateKeyException e) {
                log.info("添加专题-主键冲突 {}", e);
                return topicMapper.selectOne(queryWrap);
            }
        }
        return topic;
    }
}
