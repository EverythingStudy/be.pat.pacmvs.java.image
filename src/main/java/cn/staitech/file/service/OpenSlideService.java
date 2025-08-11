package cn.staitech.file.service;

import cn.staitech.file.domain.Image;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.util.List;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2025/4/22 09:32:40
 */
public interface OpenSlideService {

    /**
     * 把缩略图实际存储到本地
     *
     * @param inFile
     * @param id
     * @return
     */
    void processThumbUpdate(File inFile, Long id);

    void processThumb(List<Image> images) throws Exception;

    void processThumb(Image image) throws Exception;

    void reparse(List<Long> imageIds) throws Exception;

    /**
     * 单独处理切片任务
     * @param images 图像列表
     * @throws Exception 异常
     */
    void processTiles(List<Image> images) throws Exception;

}

