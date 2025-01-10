package cn.staitech.file.service.remote;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import cn.staitech.common.core.constant.SecurityConstants;
import cn.staitech.common.core.domain.R;

/**
 * 
* @ClassName: SlideImageService
* @Description:
* @author wanglibei
* @date 2024年6月6日
* @version V1.0
 */
@FeignClient(contextId = "asyncExecutorOperationLog", value = "staitech-slide")
public interface SlideImageService {
	@PostMapping("/slideFile/operationLog")
	R operationLog(@RequestBody Object object, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

//	@PostMapping("/projectRole/add")
//	SysProjectRole add(@RequestParam("projectRoleInsertVO") ProjectRoleInsertVO projectRoleInsertVO);

}
