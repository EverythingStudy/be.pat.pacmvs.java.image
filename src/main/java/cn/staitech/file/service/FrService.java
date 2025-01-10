package cn.staitech.file.service;


import cn.staitech.file.domain.Image;

/**
 * 
* @ClassName: FrService
* @Description:
* @author wanglibei
* @date 2024年3月29日
* @version V1.0
 */
public interface FrService {
    /**
     * 
    * @Title: clarityRecognition
    * @Description: 原始切片-图片清晰度校验
    * @param @param parmMap
    * @return void
    * @throws
     */
    void verification(Image image);
    
}
