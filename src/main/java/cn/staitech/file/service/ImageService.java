package cn.staitech.file.service;

import cn.staitech.common.core.domain.R;
import cn.staitech.file.domain.Image;
import cn.staitech.file.domain.Topic;
import cn.staitech.file.vo.FileInformationOutVO;
import cn.staitech.file.vo.FileInformationVO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * ImageService
 *
 * @author wangf
 */
public interface ImageService extends IService<Image> {

    Topic getTopic(String topicName, Integer projectTypeId);

    /**
     * 上传文件前置信息,向数据库中增加一条图像信息,初始化,存入MD5等信息
     *
     * @param fileInformation
     * @return
     */
    R<FileInformationOutVO> insert(FileInformationVO fileInformation) throws Exception;

    /**
     * 检查文件名及MD5是否存在
     *
     * @param req
     * @return
     */
    boolean checkImageNameAndMd5(FileInformationVO req);

}
