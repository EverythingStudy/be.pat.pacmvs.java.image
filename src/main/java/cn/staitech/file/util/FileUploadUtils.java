package cn.staitech.file.util;

import cn.staitech.common.core.exception.file.FileNameLengthLimitExceededException;
import cn.staitech.common.core.exception.file.FileSizeLimitExceededException;
import cn.staitech.common.core.exception.file.InvalidExtensionException;
import cn.staitech.common.core.utils.DateUtils;
import cn.staitech.common.core.utils.StringUtils;
import cn.staitech.common.core.utils.file.FileTypeUtils;
import cn.staitech.common.core.utils.file.MimeTypeUtils;
import cn.staitech.common.core.utils.uuid.Seq;
import cn.staitech.file.domain.Image;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.openslide.OpenSlide;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Objects;

@Slf4j

/**
 * 文件上传工具类
 *
 * @author staitech
 */ public class FileUploadUtils {

    /**
     * 允许上传的文件的扩展名
     */
    public static final String[] DEFAULT_ALLOWED_EXTENSION = {"svs", "ndpi", "tiff", "tif", "zvi", "scn", "bmp", "gif", "jpg", "jpeg", "png"};

    /**
     * 默认大小 1000M
     */
    public static final long DEFAULT_MAX_SIZE = 1000 * 1024 * 1024;

    /**
     * 默认的文件名最大长度 100
     */
    public static final int DEFAULT_FILE_NAME_LENGTH = 100;


    /**
     * 根据文件路径上传
     *
     * @param image 文件对象
     * @param file  上传的文件MultipartFile
     * @return 文件绝对路径
     * @throws IOException
     */
    public static final String upload(Image image, MultipartFile file) throws IOException {
        try {
            return upload(image, file, MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION);
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * 文件上传
     *
     * @param image
     * @param file             上传的文件
     * @param allowedExtension 上传文件类型
     * @return 文件绝对路径
     * @throws Exception
     */
    public static String upload(Image image, MultipartFile file, String[] allowedExtension) throws Exception {
        String destPath = "";
        int fileNamelength = Objects.requireNonNull(file.getOriginalFilename()).length();
        if (fileNamelength > FileUploadUtils.DEFAULT_FILE_NAME_LENGTH) {
            throw new FileNameLengthLimitExceededException(FileUploadUtils.DEFAULT_FILE_NAME_LENGTH);
        }
//        进行文件大小校验
        assertAllowed(file, allowedExtension);

        String imageUrl = image.getImageUrl();  //源图片
//        String imagePath = image.getImagePath(); // wsi图片

        // 检查是否有文件夹，没有则创建
        checkDirectory(imageUrl);

        // (真实存入)拷贝
        file.transferTo(Paths.get(imageUrl));

        // 图片转换格式
        ImageConversionsionResp resp = FileUploadUtils.pictureConversion(imageUrl,destPath);
        return resp.getDestPath();
    }

    /**
     * 检查是否有文件夹，没有则创建
     *
     * @param imageUrl
     */
    public static void checkDirectory(String imageUrl) {
        // 没有文件夹则创建新文件夹
        File saveDir = new File(imageUrl);
        if (!saveDir.getParentFile().exists()) {
            saveDir.getParentFile().mkdirs();
        }
    }

    /**
     * JPG/PNG转换成tif文件
     *
     * @param srcPath
     * @return
     * @throws Exception
     */
    public static ImageConversionsionResp pictureConversion(String srcPath,String destPath) throws Exception {
        File absFile = new File(srcPath);
        OpenSlide os = null;
        destPath = srcPath;
        log.info("--------------------------------------------------------------------------------------------------");
        try{
            log.info("********************开始验证原始切片是否可以被OpenSlide解析，切片地址：[{}]*****************",srcPath);
            os = new OpenSlide(absFile);
            log.info("********************OpenSlide解析原始切片验证通过，切片地址：[{}]********************",srcPath);
        }catch (Exception e){
            log.warn("********************OpenSlide解析原始切片验证失败：[{}]，切片地址：[{}]********************",e.getMessage(),srcPath);
            if (os!=null){
                os.dispose();
                log.warn("********************OpenSlide解析原始切片验证失败，关闭OpenSlide，切片地址：[{}]********************",e.getMessage(),srcPath);
            }
            os=null;
            log.info("********************原始切片转换，切片地址：[{}]********************",srcPath);
            destPath = srcPath.substring(0, srcPath.indexOf(".")) + ".tif";
            // 把不能识别的图片转换成可以识别的tif
            VipsUtils.convertToPyramidalTIFF(srcPath, destPath);
            log.info("********************原始切片转换完成，原地址：[{}]，转换后地址：[{}]********************",srcPath,destPath);
        }finally {
            log.info("--------------------------------------------------------------------------------------------------");
        }
        return ImageConversionsionResp.builder().openSlide(os).destPath(destPath).build();
    }

    /**
     * 编码文件名
     */
    public static final String extractFilename(MultipartFile file) {
        return StringUtils.format("{}/{}_{}.{}", DateUtils.datePath(), FilenameUtils.getBaseName(file.getOriginalFilename()), Seq.getId(Seq.uploadSeqType), FileTypeUtils.getExtension(file));
    }

    private static final File getAbsoluteFile(String uploadDir, String fileName) throws IOException {

        File desc = new File(uploadDir + File.separator + fileName);

        if (!desc.exists()) {
            if (!desc.getParentFile().exists()) {
                desc.getParentFile().mkdirs();
            }
        }
        return desc.isAbsolute() ? desc : desc.getAbsoluteFile();
    }

    public static File getAbsoluteFile(String upload) throws IOException {

        File desc = new File(upload);

        if (!desc.exists()) {
            if (!desc.getParentFile().exists()) {
                desc.getParentFile().mkdirs();
            }
        }
        return desc.isAbsolute() ? desc : desc.getAbsoluteFile();
    }

    private static final String getPathFileName(String fileName) throws IOException {
        String pathFileName = "/" + fileName;
        return pathFileName;
    }

    /**
     * 文件大小校验
     *
     * @param file 上传的文件
     * @throws FileSizeLimitExceededException 如果超出最大大小
     * @throws InvalidExtensionException      文件校验异常
     */
    public static final void assertAllowed(MultipartFile file, String[] allowedExtension) throws FileSizeLimitExceededException, InvalidExtensionException {
        long size = file.getSize();
        if (size > DEFAULT_MAX_SIZE) {
            throw new FileSizeLimitExceededException(DEFAULT_MAX_SIZE / 1024 / 1024);
        }

        String fileName = file.getOriginalFilename();
        String extension = FileTypeUtils.getExtension(file);
        if (allowedExtension != null && !isAllowedExtension(extension, allowedExtension)) {
            if (allowedExtension == MimeTypeUtils.IMAGE_EXTENSION) {
                throw new InvalidExtensionException.InvalidImageExtensionException(allowedExtension, extension, fileName);
            } else if (allowedExtension == MimeTypeUtils.FLASH_EXTENSION) {
                throw new InvalidExtensionException.InvalidFlashExtensionException(allowedExtension, extension, fileName);
            } else if (allowedExtension == MimeTypeUtils.MEDIA_EXTENSION) {
                throw new InvalidExtensionException.InvalidMediaExtensionException(allowedExtension, extension, fileName);
            } else if (allowedExtension == MimeTypeUtils.VIDEO_EXTENSION) {
                throw new InvalidExtensionException.InvalidVideoExtensionException(allowedExtension, extension, fileName);
            } else {
                throw new InvalidExtensionException(allowedExtension, extension, fileName);
            }
        }
    }

    /**
     * 判断MIME类型是否是允许的MIME类型
     *
     * @param extension        上传文件类型
     * @param allowedExtension 允许上传文件类型
     * @return true/false
     */
    public static final boolean isAllowedExtension(String extension, String[] allowedExtension) {
        for (String str : allowedExtension) {
            if (str.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断文件扩展名对应的文件是否允许上传
     *
     * @param fileName 文件名称
     * @return true可上传, false不可上传
     */
    public static boolean isAllowedExtension(String fileName) {
        // 文件扩展名
        String extension = FilenameUtils.getExtension(fileName.trim()).toLowerCase();
        for (String str : DEFAULT_ALLOWED_EXTENSION) {
            if (str.equalsIgnoreCase(extension)) {
                return false;
            }
        }
        return true;
    }


    /**
     * 获取去掉后缀的文件扩展名
     *
     * @param fileName
     * @return
     */
    public static String getFileName(String fileName) {
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(0, dotIndex);
        }
        return fileName;
    }
}