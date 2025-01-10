package cn.staitech.file.service;

import cn.staitech.common.core.domain.R;
import cn.staitech.file.vo.Chunk;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public interface FileService {
    
    boolean mergeChunk(Chunk chunk) throws IOException, InterruptedException;
    
    
}
