package cn.staitech.file.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "python",url = "${frinspection.image-verification}")
public interface PythonService {
    
    @PostMapping(value = "CreateCPUAIPepost/")
    String startPrediction(@RequestBody StartRecognition startRecognition);


}
