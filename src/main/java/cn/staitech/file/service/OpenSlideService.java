package cn.staitech.file.service;

import cn.staitech.file.domain.Image;
import org.openslide.OpenSlide;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2025/4/22 09:32:40
 */
public interface OpenSlideService {


    /**
     * 本地文件上传接口
     *
     * @param file    上传的文件
     * @param imageId
     * @return 访问地址
     * @throws Exception
     */
    String uploadFile(MultipartFile file, Long imageId) throws Exception;

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

}

