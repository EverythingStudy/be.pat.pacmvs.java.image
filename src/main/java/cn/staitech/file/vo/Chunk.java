package cn.staitech.file.vo;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;

/**
 * @author wangf
 */
@Data
@Accessors(chain = true)
public class Chunk implements Serializable {
    /**
     * 当前文件块，从0开始
     */
    private Integer chunkNumber;
    /**
     * 分块大小
     */
    private Long chunkSize;
    /**
     * 总大小
     */
//    private Long totalSize;

    /**
     * 文件名
     */
    private String filename;
    /**
     * 相对路径
     */
//    private String relativePath;
    /**
     * 总块数
     */
    private Integer totalChunks;
    
    /**
     * 文件Id:guid
     */
    private Long imageId;
    
    /**
     * 二进制文件
     */
    private MultipartFile file;
}