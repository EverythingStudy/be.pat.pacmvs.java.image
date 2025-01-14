package cn.staitech.file.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.staitech.common.core.domain.R;
import cn.staitech.common.core.utils.uuid.IdUtils;
import cn.staitech.common.security.utils.SecurityUtils;
import cn.staitech.file.constants.DataConstants;
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
import cn.staitech.system.api.domain.SysUser;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Date;
import java.util.List;
import java.util.Set;


/**
 * ImageServiceImpl
 *
 * @author wangf
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
        Integer bizType = fileInformation.getProjectTypeId() > 0 ? fileInformation.getProjectTypeId() : 1;
        // 项目类型(为topic类型特殊处理)
        Integer projectTypeId = (bizType == 7) ? 6 : bizType;
        // 向MySQL插入数据 - 生成文件目录、文件名等
        // 1、 the first insert into image table get imageId
        // 2、 Produce image`s paths  ->savePath()
        // 3、 Update image table
        // 4、Add Image to Redis
        Long userId = SecurityUtils.getUserId();
        Image image = new Image();
        // 浅拷贝，把req里面的值拷贝到image中，利用封装的SpringUtils，进行只有非null值覆盖
        BeanUtil.copyProperties(fileInformation,image);

        try {
            String topicCode = image.getTopicName();
            //处理专题数据
            //Topic topic = getTopic(topicCode);
            Topic topic = getTopic(topicCode, projectTypeId);
            image.setBizType(bizType);
            image.setTopicId(topic.getTopicId());
            image.setTopicName(topicCode);
            image.setOrganizationId(topic.getOrganizationId());
            image.setStatus(ImageConstant.IMAGE_STATUS_UNABLE);
            //image.setProcessFlag(ImageConstant.IMAGE_PROCESS_PARSING);
            image.setImageCode(IdUtils.randomUUID());
            image.setUpdateBy(userId);
            image.setCreateBy(userId);
            image.setSource(ImageConstant.IMAGE_SOURCE_UPLOAD);
            // 逻辑删除状态（0删除，1未删除）
            image.setDelFlag(DataConstants.NOT_DELETED);
            // 去掉文件扩展名的文件名称
            image.setFileName(FileUploadUtils.getFileName(fileInformation.getImageName()));
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
        }catch (Exception e){
            e.printStackTrace();
            log.error("FileInformation信息创建异常：{};;{}",e.getMessage(),fileInformation);
            throw e;
        }
        return R.fail("补充信息失败");
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
        //String newFileName = filePrefix + "_" + imageId + "." + image.getFormat();
        simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        String folderName = simpleDateFormat.format(currentTime);
        // String imagePathDir = localFilePath + "/" + image.getTopicName() + "/big/" + folderName + "/";
        String imagePathDir = localFilePath + "/" + image.getTopicName() + "/";
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
        String filePathStr = folderName + "/" + imageId + "/0.jpg";
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
                .delFlag(DataConstants.NOT_DELETED).build()));
        if (images.isEmpty()) {
            return true;
        }
        return false;
    }


    /**
     * 处理专题数据
     * @param topicName
     * @return
     */
    @Override
    public Topic getTopic(String topicName, Integer projectTypeId){
        SysUser sysUser = SecurityUtils.getLoginUser().getSysUser();
        Long userId = sysUser.getUserId();

        Topic topic = Topic.builder()
                .topicName(topicName)
                .projectTypeId(projectTypeId)
                .organizationId(sysUser.getOrganizationId())
                .build();

        QueryWrapper queryWrap = new QueryWrapper(topic);
        Topic qTopic = topicMapper.selectOne(queryWrap);

        // 有则返回
        if (qTopic != null) {
            return qTopic;
        } else { // 无则添加
            topic.setProjectTypeId(projectTypeId);
            topic.setCreateBy(userId);
            topic.setUpdateBy(userId);
            topic.setCreateTime(new Date());
            topic.setUpdateTime(new Date());
            topic.setOrganizationId(sysUser.getOrganizationId());
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
