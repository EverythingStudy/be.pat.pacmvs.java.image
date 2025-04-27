package cn.staitech.file.service;

import cn.staitech.file.vo.Chunk;
import java.io.IOException;

public interface FileService {
    
    boolean mergeChunk(Chunk chunk) throws IOException, InterruptedException;
}
