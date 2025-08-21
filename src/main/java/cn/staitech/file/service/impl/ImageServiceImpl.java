package cn.staitech.file.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import cn.staitech.common.core.domain.R;
import cn.staitech.common.core.utils.uuid.IdUtils;
import cn.staitech.common.security.utils.SecurityUtils;
import cn.staitech.file.constant.ImageConstant;
import cn.staitech.file.domain.Image;
import cn.staitech.file.util.ImageUtils;
import cn.staitech.file.domain.Topic;
import cn.staitech.file.mapper.ImageMapper;
import cn.staitech.file.mapper.TopicMapper;
import cn.staitech.file.service.ImageService;
import cn.staitech.file.vo.FileInformationOutVO;
import cn.staitech.file.vo.FileInformationVO;
import cn.staitech.file.vo.FileInsertVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.io.File;
import java.nio.file.Paths;
import java.util.*;
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

    private static Snowflake snowflake = IdUtil.getSnowflake();

    /**
     * 批量处理文件插入请求
     * <p>
     * 此方法负责接收一个包含文件信息的FileInsertVO对象，验证文件的有效性，
     * 检查文件是否已存在于数据库中，然后创建并保存Image对象到数据库
     *
     * @param vo FileInsertVO对象，包含需要插入的文件信息和组织ID
     * @return 返回保存的Image对象列表
     * @throws Exception 如果文件处理过程中发生错误，则抛出异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Image> batchFileHandle(FileInsertVO vo) throws Exception {
        // 检查文件列表是否为空
        if (vo == null || vo.getFileList() == null || vo.getFileList().length == 0) {
            throw new IllegalArgumentException("图片文件绝对路径数组不可为空");
        }

        // 验证文件路径有效性
        /*for (String path : vo.getFileList()) {
            File file = new File(path);
            if (!file.exists() || !file.isFile() || !file.canRead()) {
                throw new IllegalArgumentException("文件路径无效或不可访问: " + path);
            }
        }
        List<Image> images = new ArrayList<>();

        List<String> filePaths = Arrays.asList(vo.getFileList());
        // 有Retry状态的图像，则将其名称添加到retryImageName列表中
        List<String> retryImagePaths = filePaths.stream().filter(path -> path.contains(ImageConstant.SLIDE_STORAGE_RETRY)).collect(Collectors.toList());
        List<String> retryImageName = retryImagePaths.stream().map(path -> new File(path).getName()).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(retryImageName)) {
            // 根据retryImageName查询失败的图像
            List<Image> failImages = imageMapper.selectList(Wrappers.<Image>lambdaQuery().eq(Image::getOrganizationId, vo.getOrganizationId())
                    .in(Image::getImageName, retryImageName)
                    .in(Image::getStatus,ImageConstant.IMAGE_STATUS_MSG_PARSE_FAIL, ImageConstant.IMAGE_STATUS_PARSE_FAIL, ImageConstant.IMAGE_STATUS_TILE_PROCESS_FAIL));
            // 更新失败图像的路径和URL
            for (Image image : failImages) {
                image.setImagePath(retryImagePaths.stream().filter(path -> path.contains(image.getImageName())).findFirst().orElse(image.getImagePath()));
                image.setImageUrl(retryImagePaths.stream().filter(path -> path.contains(image.getImageName())).findFirst().orElse(image.getImagePath()));
                image.setUpdateTime(new Date());
            }
            // 批量更新失败图像
            images.addAll(failImages);
        }
        // 检查文件是否已存在于数据库
        long exists = imageMapper.selectCount(Wrappers.<Image>lambdaQuery()
                .in(Image::getImagePath, filePaths)
                .eq(Image::getOrganizationId, vo.getOrganizationId()));
        if (exists > 0) {
            List<Image> existImages = imageMapper.selectList(Wrappers.<Image>lambdaQuery()
                    .in(Image::getImagePath, filePaths)
                    .eq(Image::getOrganizationId, vo.getOrganizationId()));
            List<String> existImagePaths = existImages.stream().map(Image::getImagePath).collect(Collectors.toList());
            // 从filePaths中移除已存在的图像路径，避免重复处理
            filePaths = filePaths.stream()
                    .filter(path -> !existImagePaths.contains(path))
                    .filter(path -> !path.contains(ImageConstant.SLIDE_STORAGE_RETRY))
                    .collect(Collectors.toList());
            // 如果存在的图像中有Retry状态的图像，则将其名称添加到retryImageName列表中
            List<Image> retryFailImages = existImages.stream().filter(image -> (Objects.equals(image.getStatus(), ImageConstant.IMAGE_STATUS_PARSE_FAIL)
                    || Objects.equals(image.getStatus(), ImageConstant.IMAGE_STATUS_TILE_PROCESS_FAIL)) && image.getImagePath().contains(ImageConstant.SLIDE_STORAGE_RETRY)).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(retryFailImages)) {
                images.addAll(retryFailImages);
            }
            log.warn("服务器选片异常:[{}]文件已经存在", existImages.stream().map(Image::getImagePath).collect(Collectors.joining(", ")));
            /*throw new DuplicateKeyException("服务器选片异常:["
                    + existImages.stream().map(Image::getImagePath).collect(Collectors.joining(", "))
                    + "]文件已经存在");*/
        }

        for (String path : filePaths) {
            File file = new File(path);
            String imageName = file.getName();
            Image image = createImageFromPath(vo.getOrganizationId(), path, imageName);
            image.setStatus(ImageConstant.IMAGE_STATUS_MSG_PARSING); // 设置初始状态为解析中
            parseSlideCode(image.getFileName(), image); // 解析文件名并设置相关字段
            if (Objects.equals(image.getAnalyzeStatus(), ImageConstant.IMAGE_NAME_PARSE_FAIL)) {
                image.setStatus(ImageConstant.IMAGE_STATUS_MSG_PARSE_FAIL); // 如果解析失败，设置状态为解析失败
            }
            processImageCommon(image); // 处理图像的公共逻辑
            images.add(image);
        }

        // 批量保存图像信息到数据库
        saveOrUpdateBatch(images);
        return images;
    }

    /**
     * 根据文件路径创建 Image 对象
     * <p>
     * 此方法负责根据给定的组织ID、文件路径和文件名创建一个Image对象，并设置其基本属性
     *
     * @param organizationId 组织ID，用于关联图像到特定的组织
     * @param path           图像文件的绝对路径
     * @param imageName      图像文件的名称
     * @return 返回初始化后的Image对象
     */
    private Image createImageFromPath(Long organizationId, String path, String imageName) {
        Image image = new Image();
        image.setOrganizationId(organizationId);
        image.setImagePath(path);
        image.setImageUrl(path);
        image.setImageName(imageName);
        image.setSize(String.valueOf(new File(path).length()));
        image.setFileName(ImageUtils.getFileName(imageName));
        image.setStatus(ImageConstant.IMAGE_STATUS_PARSING);
        image.setSource(ImageConstant.IMAGE_SOURCE_SERVER);
        return image;
    }

    /**
     * 文件信息上传方法
     * 该方法负责处理文件信息的上传，包括文件信息的验证、处理和保存
     *
     * @param fileInformation 文件信息输入对象，包含需要上传的文件相关信息
     * @return 返回一个封装了处理结果的R对象，包括是否成功和附加信息
     * @throws Exception 如果在处理过程中发生异常，则抛出Exception
     */
    @Override
    public R<FileInformationOutVO> fileInformationUpload(FileInformationVO fileInformation) throws Exception {
        // 检查输入的文件信息是否为空或无效
        if (fileInformation == null || StringUtils.isBlank(fileInformation.getImageName())) {
            return R.fail("文件信息不能为空");
        }

        Image image = new Image();
        BeanUtil.copyProperties(fileInformation, image);

        try {
            // 设置默认值
            image.setOrganizationId(fileInformation.getOrganizationId());
            image.setStatus(ImageConstant.IMAGE_STATUS_UPLOADING);
            image.setSource(ImageConstant.IMAGE_SOURCE_UPLOAD);

            // 校验并处理文件名
            String fileName = validateAndExtractFileName(fileInformation.getImageName());
            if (fileName == null) {
                return R.fail("文件名格式不正确");
            }
            image.setFileName(fileName);

            // 拆分图片名称字段
            parseSlideCode(fileName, image);

            processImageCommon(image);

            // 初始化文件路径
            String imagePathDir = initializeFilePath(localFilePath, image.getOrganizationId(), image.getTopicName());
            String imagePath = imagePathDir + File.separator + image.getImageName();
            image.setImagePath(imagePath);
            image.setImageUrl(imagePath);

            // 创建目录
            File imageDir = new File(imagePathDir);
            if (!imageDir.exists()) {
                if (!imageDir.mkdirs()) {
                    log.error("无法创建目录: {}", imagePathDir);
                    return R.fail("文件目录创建失败");
                }
            }

            // 插入数据库
            int insert = imageMapper.insert(image);
            if (insert <= 0) {
                log.error("插入数据库失败: {}", image);
                return R.fail("文件信息保存失败");
            }

            // 构造返回值
            FileInformationOutVO out = new FileInformationOutVO();
            out.setImageId(image.getImageId());
            return R.ok(out, "补充信息成功");

        } catch (Exception e) {
            log.error("[{}] 补充信息失败,[{}]", fileInformation, e.getMessage());
            return R.fail("系统未知错误");
        }
    }

    /**
     * 校验并提取文件名
     * 该方法接受一个文件名字符串，验证其有效性并提取文件名
     *
     * @param imageName 文件名字符串
     * @return 如果文件名有效，则返回提取后的文件名；否则返回null
     */
    private String validateAndExtractFileName(String imageName) {
        if (StringUtils.isBlank(imageName)) {
            return null;
        }
        String fileName = ImageUtils.getFileName(imageName);
        if (StringUtils.isBlank(fileName)) {
            return null;
        }
        return fileName;
    }

    /**
     * 初始化文件路径
     * 根据基础路径、组织ID和主题名称生成文件的保存路径
     *
     * @param basePath       基础路径
     * @param organizationId 组织ID
     * @param topicName      主题名称
     * @return 返回生成的文件保存路径字符串
     * @throws IllegalArgumentException 如果组织ID或主题名称为空，则抛出该异常
     */
    private String initializeFilePath(String basePath, Long organizationId, String topicName) {
        if (organizationId == null || StringUtils.isBlank(topicName)) {
            throw new IllegalArgumentException("组织ID或主题名称不能为空");
        }
        return Paths.get(basePath, ImageUtils.getFourNumber(organizationId), topicName).toString();
    }

    /**
     * 处理图像的通用方法
     * 设置图像的基本信息，包括图像代码、更新者、创建者等，并生成相应的URL
     *
     * @param image 待处理的图像对象，包含图像的基本信息如名称、组织ID等
     */
    private void processImageCommon(Image image) {
        // 获取用户ID并校验
        Long userId = 1L;
        try {
            userId = SecurityUtils.getUserId();
        }catch (NullPointerException e) {
            log.error("获取用户ID失败，使用默认用户ID: {}", userId);
        }

        // 设置图像的基本信息
        image.setImageCode(IdUtils.randomUUID());
        image.setUpdateBy(userId);
        image.setCreateBy(userId);

        // 提取文件格式并校验
        String imageName = image.getImageName();
        if (imageName == null || !imageName.contains(".")) {
            throw new IllegalArgumentException("Image name is invalid or missing file extension");
        }
        image.setFormat(imageName.substring(imageName.lastIndexOf('.') + 1));

        // 构造文件路径
        String filePathStr = generateFilePathStr();
        String organizationPath = generateOrganizationPath(image.getOrganizationId());

        // 设置不同类型的URL
        image.setThumbUrl(generatePath(organizationPath, "thumbnail", filePathStr));
        image.setMacroUrl(generatePath(organizationPath, "macro", filePathStr));
        image.setLabelUrl(generatePath(organizationPath, "label", filePathStr));
        image.setCacheUrl(generatePath(organizationPath, "cacheThumbnail", filePathStr));
    }

    /**
     * 生成文件路径字符串
     * 使用Snowflake算法生成一个唯一的ID作为文件名，确保文件名不重复
     *
     * @return 文件路径字符串，格式为唯一的ID加上固定的文件名"0.jpg"
     */
    private String generateFilePathStr() {
        return snowflake.nextIdStr() + File.separator + "0.jpg";
    }

    /**
     * 生成组织路径
     * 根据组织ID生成路径，用于存储该组织相关的图像文件
     *
     * @param organizationId 组织的唯一标识符，用于生成路径
     * @return 组织路径字符串，格式为固定的基目录加上组织ID的前四位数字
     */
    private String generateOrganizationPath(Long organizationId) {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        return ImageConstant.THUMB_BASE_DIR + File.separator + ImageUtils.getFourNumber(organizationId);
    }

    /**
     * 生成具体路径
     * 根据基础路径、类型和文件路径字符串生成完整的路径
     *
     * @param basePath    基础路径，可以是组织路径或其他基础目录路径
     * @param type        文件类型，如"thumbnail"、"macro"等
     * @param filePathStr 文件路径字符串，包含文件名
     * @return 完整的路径字符串，格式为基础路径加上类型和文件路径字符串
     */
    private String generatePath(String basePath, String type, String filePathStr) {
        return basePath + File.separator + type + File.separator + filePathStr;
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
        image.setAnalyzeStatus(ImageConstant.IMAGE_NAME_PARSE_SUCC);
        try {
            if (parts.length >= 3 && parts.length <= 5) {
                // 解析专题号部分
                String topicNumber = parts[0].trim();
                image.setTopicName(topicNumber);
                Topic topic = getTopic(topicNumber,image.getOrganizationId());
                if (topic != null) {
                    image.setTopicId(topic.getTopicId());
                } else {
                    log.error("未找到专题信息，专题号: {}", topicNumber);
                    image.setAnalyzeStatus(ImageConstant.IMAGE_NAME_PARSE_FAIL);
                }

                // 解析动物号和蜡块号部分
                String animalAndWaxBlock = parts[1].trim();
                int lastIndex = animalAndWaxBlock.lastIndexOf('-');
                if (lastIndex == -1 || lastIndex == animalAndWaxBlock.length() - 1) {
                    log.error("动物号和蜡块号格式无效: {}", animalAndWaxBlock);
                    image.setAnalyzeStatus(ImageConstant.IMAGE_NAME_PARSE_FAIL);
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
                    if (!ImageConstant.FEMALE.equals(gender) && !ImageConstant.MALE.equals(gender)) {
                        log.error("性别格式无效: {}", groupNumberAndGender);
                        image.setAnalyzeStatus(ImageConstant.IMAGE_NAME_PARSE_FAIL);
                    }
                    image.setGroupCode(groupNumber);
                    image.setSexFlag(gender);
                } else {
                    log.error("组号和性别格式无效: {}", groupNumberAndGender);
                    image.setAnalyzeStatus(ImageConstant.IMAGE_NAME_PARSE_FAIL);
                }
                // 解剖期限
                if (parts.length >= 4) {
                    String period = parts[3].trim();
                    String periodResult = Arrays.stream(ImageConstant.ANATOMY_PERIOD_CONSTANT).filter(s -> period.contains(s)).findAny().orElse("");
                    image.setPeriod(periodResult);
                }
            } /*else {
                parseSlideCode(input, image);
            }*/
        } catch (Exception e) {
            image.setAnalyzeStatus(ImageConstant.IMAGE_NAME_PARSE_FAIL);
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
     * @param inputSrc 输入字符串，格式为"专题号~动物号~蜡块号~组别性别~其他尾缀_时间戳"
     * @param image    图像对象，用于存储解析结果
     * @return 更新后的图像对象
     * @throws Exception 如果解析失败，抛出异常
     */
    private Image parseSlideCode(String inputSrc, Image image) throws Exception {
        // 输入参数校验
        if (inputSrc == null || inputSrc.isEmpty()) {
            log.error("切片编号解析失败：输入为空");
            image.setAnalyzeStatus(ImageConstant.IMAGE_NAME_PARSE_FAIL);
            return image;
        }
        try {
            // 根据波浪号拆分字符串为五个主要部分
            String input = StringUtils.replace(inputSrc, " ", "");
            StringUtils.trimToEmpty(input);
            //R24-224-RD～2424912～2～4M～~TN～RC-1_083944]~~~~~~~~~~~~~~~~~~~~~~~
            String[] parts = input.split("~");
            // 文件名解析状态: 默认1成功
            image.setAnalyzeStatus(ImageConstant.IMAGE_NAME_PARSE_SUCC);
            if (parts.length == 4 || parts.length == 5 || parts.length == 6) {
                // 解析专题号部分
                String topicNumber = parts[0].trim();
                image.setTopicName(topicNumber);
                Topic topic = getTopic(topicNumber, image.getOrganizationId());
                if (topic != null) {
                    image.setTopicId(topic.getTopicId());
                } else {
                    log.error("切片编号：[{}], 未找到专题信息，专题号: {}", input, topicNumber);
                    image.setAnalyzeStatus(ImageConstant.IMAGE_NAME_PARSE_FAIL);
                }
                // 解析动物号和蜡块号部分
                String animalCode = parts[1].trim();
                image.setAnimalCode(animalCode);
                String waxBlock = parts[2].trim();
                image.setWaxCode(waxBlock);
                // 解析组号和性别部分
                String groupNumberAndGender = parts[3].trim();
                // 解析组号和性别部分
                GroupAndSex parsedGroupNumber = extractGroupNumber(groupNumberAndGender);
                if (parsedGroupNumber == null || (!ImageConstant.FEMALE.equals(parsedGroupNumber.getSex()) && !ImageConstant.MALE.equals(parsedGroupNumber.getSex()))) {
                    log.error("切片编号：[{}], 组号和性别格式无效: {}", input, groupNumberAndGender);
                    image.setAnalyzeStatus(ImageConstant.IMAGE_NAME_PARSE_FAIL);
                    return image;
                }
                image.setGroupCode(parsedGroupNumber.getGroupCode());
                image.setSexFlag(parsedGroupNumber.getSex());
                // 解剖期限
                for (int i = 4; i < parts.length; i++) {
                    String period = parts[i].trim();
                    String periodResult = Arrays.stream(ImageConstant.ANATOMY_PERIOD_CONSTANT).filter(s -> period.contains(s)).findAny().orElse("");
                    image.setPeriod(periodResult);
                    if (StringUtils.isNotBlank(periodResult)) {
                        break;
                    }
                }
            } else {
                log.error("切片编号：[{}],输入格式无效", input);
                image.setAnalyzeStatus(ImageConstant.IMAGE_NAME_PARSE_FAIL);
            }
        } catch (Exception e) {
            log.error("切片编号解析异常：{}, 输入: {}", e.getMessage(), inputSrc, e);
            image.setAnalyzeStatus(ImageConstant.IMAGE_NAME_PARSE_FAIL);
        }
        if (image.getAnalyzeStatus() == ImageConstant.IMAGE_NAME_PARSE_FAIL) {
            parseFields(inputSrc, image);
        }
        return image;
    }


    // 提取公共方法：解析组号和性别
    private GroupAndSex extractGroupNumber(String groupNumberAndGender) {
        if (groupNumberAndGender == null || groupNumberAndGender.isEmpty()) {
            return null;
        }

        int underscoreIndex = groupNumberAndGender.indexOf("_");
        if (underscoreIndex != -1) {
            groupNumberAndGender = groupNumberAndGender.substring(0, underscoreIndex);
        }

        String gender = groupNumberAndGender.substring(groupNumberAndGender.length() - 1);
        if (!"M".equals(gender) && !"F".equals(gender)) {
            return null;
        }

        String groupCode = groupNumberAndGender.substring(0, groupNumberAndGender.length() - 1);
        return new GroupAndSex(groupCode, gender);
    }


    // 内部类：用于封装组号和性别解析结果
    private static class GroupAndSex {
        private final String groupCode;
        private final String sex;

        public GroupAndSex(String groupCode, String sex) {
            this.groupCode = groupCode;
            this.sex = sex;
        }

        public String getGroupCode() {
            return groupCode;
        }

        public String getSex() {
            return sex;
        }
    }

    private Topic getTopic(String topicName,Long organizationId) {
        // 校验输入参数
        if (topicName == null || topicName.isEmpty()) {
            throw new IllegalArgumentException("专题不可为空");
        }
        Long userId = 1L;
        try {
            userId = SecurityUtils.getUserId();
        }catch (NullPointerException e) {
            log.error("获取用户ID失败，使用默认用户ID: {}", userId);
        }
        Topic topic = topicMapper.selectOne(Wrappers.<Topic>lambdaQuery().eq(Topic::getTopicName, topicName)
                .eq(Topic::getOrganizationId, organizationId));
        if (topic == null) {
            topic = Topic.builder().topicName(topicName).organizationId(organizationId).createBy(userId)
                    .updateBy(userId).createTime(new Date()).updateTime(new Date()).build();
            topicMapper.insert(topic);
        }
        return topic;
    }
}
