package cn.staitech.file.util;

import cn.staitech.file.vo.ImageConversionsionResp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.openslide.OpenSlide;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.NumberFormat;


/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2025/4/22 09:32:40
 */
@Slf4j
public class ImageUtils {

    /**
     * 允许上传的文件的扩展名
     */
    public static final String[] DEFAULT_ALLOWED_EXTENSION = {"svs", "ndpi", "tiff", "tif", "zvi", "scn", "bmp", "gif", "jpg", "jpeg", "png"};

    /**
     * 默认大小 1000M
     */
    public static final long DEFAULT_MAX_SIZE = 1000 * 1024 * 1024;

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
    public static ImageConversionsionResp pictureConversion(String srcPath) throws Exception {
        // 参数校验
        if (srcPath == null) {
            throw new IllegalArgumentException("Source path or destination path is null.");
        }
        String destPath = srcPath.substring(0, srcPath.lastIndexOf(".")) + ".tif";

        File imagesFile = new File(srcPath);
        if (!imagesFile.exists()) {
            throw new FileNotFoundException("Source file does not exist: " + srcPath);
        }

        OpenSlide os = null;

        try {
            log.info("开始验证原始切片是否可以被OpenSlide解析，切片地址：{}", srcPath);
            os = new OpenSlide(imagesFile);
            log.info("OpenSlide解析原始切片验证通过，切片地址：{}", srcPath);

        } catch (Exception e) {
            log.error("OpenSlide解析原始切片验证失败：{}，切片地址：{}", e.getMessage(), srcPath);
            if (os != null) {
                os.dispose();
                log.error("OpenSlide解析原始切片验证失败，已关闭OpenSlide，切片地址：{}", srcPath);
            }
            os = null;

            log.info("原始切片转换，切片地址：{}", srcPath);
            // 把不能识别的图片转换成可以识别的tif
            VipsUtils.convertToPyramidalTIFF(srcPath, destPath);
            os = new OpenSlide(new File(destPath));
            log.info("原始切片转换完成，原地址：{}，转换后地址：{}", srcPath, destPath);
        }finally {
            if (os != null) {
                int levelCount = os.getLevelCount();
                log.info("验证切片元数据 levelCount：{}", levelCount);
                if (levelCount <= 0) {
                    os.dispose();
                    os = new OpenSlide(imagesFile);
                    log.info("切片元数据 levelCount：{} OpenSlide重新加载切片，切片地址：{}",
                            os.getLevelCount(), srcPath);
                }
            }
        }
        return ImageConversionsionResp.builder().openSlide(os).destPath(destPath).build();
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

    /**
     * 生成四位数的文件夹路径
     * @param number
     * @return
     */
    public static String getFourNumber(Long number) {
        return getOrgIdFormat(number) + File.separator +"Slides";
    }

    public static String getOrgIdFormat(Long number) {
        NumberFormat formatter = NumberFormat.getNumberInstance();
        formatter.setMinimumIntegerDigits(3);
        formatter.setGroupingUsed(false);
        return "C" + formatter.format(number);
    }


}