package cn.staitech.file.scheduled;

import cn.hutool.core.date.DateUtil;
import cn.staitech.file.domain.Image;
import cn.staitech.file.service.ImageService;
import cn.staitech.file.service.OpenSlideService;
import cn.staitech.file.vo.FileInsertVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ImageScannerComp {

    @Value("${file.scanPath}")
    private String rootDirStr;
    @Value("${file.path}")
    private String localFilePath;

    @Resource
    private ImageService imageService;
    @Resource
    private OpenSlideService openSlideService;

    /**
     * 定时扫描图像目录
     */
//    @Scheduled(fixedRate = 1000*60*60)
    @Scheduled(fixedRate = 1000*60)
    public void imageScanner() {
//        String rootDirStr = "";
//        String redisKey = "image:scanner:organization:dir";
        //扫描根目录下文件夹，文件夹命名规则C001，C012等，解析出数字
        File rootDir = new File(localFilePath);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            log.error("根目录不存在或不是一个目录: {}", rootDirStr);
            return;
        }
        File[] subDirs = rootDir.listFiles(File::isDirectory);
        if (subDirs == null || subDirs.length == 0) {
            log.info("根目录下没有子目录");
            return;
        }
        for (File subDir : subDirs) {
            String dirName = subDir.getName();
            if (dirName.startsWith("C") && dirName.length() > 1) {
                String numberStr = dirName.substring(1);
                try {
                    Long number = Long.parseLong(numberStr);
                    // 检查是否存在对应的组织
                    if (number > 0) {
                        // 如果存在，继续处理
                        log.info("处理机构目录: {}, 解析出的数字: {}", dirName, number);
                        FileInsertVO result = new FileInsertVO();
                        scanOrganizationDir(subDir, number, result);
                        List<Image> images = imageService.batchFileHandle(result);
                        openSlideService.processThumb(images);
                    }
                } catch (Exception e) {
                    log.error("机构目录名称解析失败: {}, 错误信息: {}", dirName, e.getMessage());
                    continue;
                }
            } else {
                log.warn("目录名称不符合预期格式: {}", dirName);
            }
            log.info("处理完成");
        }
    }

    /**
     * 扫描组织目录下的原始切片
     * @param rootDir
     * @param organizationId
     * @param result
     */
    private void scanOrganizationDir(File rootDir, Long organizationId, FileInsertVO result) {
        // 计算一小时前的时间戳
//        long oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000);
        long oneHourAgo = System.currentTimeMillis() - (10 * 1000);
        List<String> imagePaths = new ArrayList<>();
        final String slideDirStr = "Slides";
        final String retryDirStr = "Retry";

        // 扫描slideDirStr目录，遍历文件夹
        File slideDir = new File(rootDir, slideDirStr);
        if (!slideDir.exists() || !slideDir.isDirectory()) {
            log.error("切片目录不存在或不是一个目录: {}", slideDirStr);
            return;
        }

        File[] scannerDirs = slideDir.listFiles(File::isDirectory);
        if (scannerDirs == null || scannerDirs.length == 0) {
            log.info("切片目录下没有子目录");
            return;
        }

        // 遍历扫描仪目录
        for (File scannerDir : scannerDirs) {
            try {
                log.info("处理扫描仪目录: {}", scannerDir.getAbsolutePath());
                File retryDir = new File(slideDir, retryDirStr);
                if (scannerDir.equals(retryDir)) {
                    // 如果是重试目录，直接添加其中的图像文件
                    log.info("处理重试目录: {}", retryDir.getAbsolutePath());
                    if (retryDir.exists() && retryDir.isDirectory()) {
                        File[] retryFiles = retryDir.listFiles((dir, name) -> name.endsWith(".svs"));
                        if (retryFiles != null && retryFiles.length > 0) {
                            for (File retryFile : retryFiles) {
                                if (retryFile.lastModified() > oneHourAgo) {
                                    log.info("跳过重试目录中的图像文件: {}, 因为它的创建时间晚于一小时前", retryFile.getName());
                                    continue;
                                }
                                imagePaths.add(retryFile.getAbsolutePath());
                            }
                        } else {
                            log.info("重试目录下没有图像文件");
                        }
                    } else {
                        log.warn("重试目录不存在或不是一个目录: {}", retryDirStr);
                    }
                    continue;
                }
                // 修复逻辑错误：应使用 scannerDir 而非 slideDir
                File[] imageDirs = scannerDir.listFiles(File::isDirectory);
                if (imageDirs == null || imageDirs.length == 0) {
                    log.info("扫描仪目录下没有子目录");
                    continue;
                }

                File todayDir = new File(scannerDir, DateUtil.today());
                if (!todayDir.exists() || !todayDir.isDirectory()) {
                    log.warn("当天的目录不存在或不是一个目录: {}", todayDir.getAbsolutePath());
                    continue;
                }

                File[] imageFiles = todayDir.listFiles((dir, name) -> name.endsWith(".svs"));
                if (imageFiles == null || imageFiles.length == 0) {
                    log.info("当天目录下没有图像文件");
                    continue;
                }

                // 遍历图像文件
                for (File imageFile : imageFiles) {
                    if (imageFile.lastModified() > oneHourAgo) {
                        log.info("跳过图像文件: {}, 因为它的创建时间晚于一小时前", imageFile.getName());
                        continue;
                    }
                    imagePaths.add(imageFile.getAbsolutePath());
                }
            } catch (SecurityException e) {
                log.error("访问目录时发生安全异常: {}", scannerDir.getAbsolutePath(), e);
            }
        }

        // 将图像路径添加到结果中
        if (!imagePaths.isEmpty()) {
            result.setFileList(imagePaths.toArray(new String[imagePaths.size()]));
            result.setOrganizationId(organizationId);
        } else {
            log.info("没有找到任何图像文件");
        }
    }


}
