package cn.staitech.file.service.impl;

import cn.hutool.json.JSONUtil;
import cn.staitech.file.DataConstants;
import cn.staitech.file.domain.Image;
import cn.staitech.file.feign.PythonService;
import cn.staitech.file.feign.StartRecognition;
import cn.staitech.file.service.FrService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.text.NumberFormat;

@Slf4j
@Service
public class FrServiceImpl implements FrService {

    @Resource
    private PythonService pythonService;
    
    /**
     * 智能阅片check
     */
	@Override
	public void verification(Image image) {
		StartRecognition startRecognition = new StartRecognition();
		BeanUtils.copyProperties(image, startRecognition);
		startRecognition.setAlgorithm_name(DataConstants.ALGORITHM_MODEL_NAME);
		String number = geNumber(image.getOrganizationId());
		startRecognition.setOrganizationName(number);
		String reqData = JSONUtil.toJsonStr(startRecognition);
		log.info("AI清晰度算法请求imageId:{},完整信息：{}",image.getImageId(), reqData);
		//请求算法接口
		try {

			String retInfo = pythonService.startPrediction(startRecognition);
			log.info("AI清晰度算法请求返回数据{}", JSONUtil.toJsonStr(retInfo));
		} catch (Exception e) {
			log.error("AI清晰度算法请求数据：错误imageId:{},信息：{}",image.getImageId(), e.getMessage());
			e.printStackTrace();
		}finally {

		}
	}
	
	public  String geNumber(Long organizationId) {
		NumberFormat formatter = NumberFormat.getNumberInstance();
		formatter.setMinimumIntegerDigits(3);
		formatter.setGroupingUsed(false);
		return "C" + formatter.format(organizationId);
	}
}
