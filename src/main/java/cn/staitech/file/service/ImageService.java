package cn.staitech.file.service;

import cn.staitech.common.core.domain.R;
import cn.staitech.file.domain.Image;
import cn.staitech.file.vo.FileInformationOutVO;
import cn.staitech.file.vo.FileInformationVO;
import cn.staitech.file.vo.FileInsertVO;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2025/4/22 09:32:40
 */
public interface ImageService extends IService<Image> {

    List<Image> batchFileHandle(FileInsertVO vo) throws Exception;

    R<FileInformationOutVO> fileInformationUpload(FileInformationVO fileInformation) throws Exception;

}
