package cn.staitech.file.service;

import cn.staitech.file.domain.Image;
import cn.staitech.file.vo.FileInsertVO;
import com.google.common.cache.Cache;
import org.openslide.OpenSlide;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;


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
     * 根据物理地址,获取到病理图片的resolutionX,resolutionY,sourceLens并存入image
     * @param os
     * @param image
     * @throws IOException
     */

    void writeRseolution(OpenSlide os, Image image) throws IOException;


    /**
     * 把缩略图实际存储到本地
     *
     * @param inFile
     * @param id
     * @return
     */
    void processThumbUpdate(File inFile, Long id);
    void processThumbSave(File inFile, Image image);

    /**
     * 异步批量服务器读取切片
     * @param vo
     * @throws Exception
     */
    void asynSaveBatch(FileInsertVO vo) throws Exception;

    void reparse(List<Long> imageIds) throws Exception;

    Cache<String, Object> getCache() throws Exception;

}

