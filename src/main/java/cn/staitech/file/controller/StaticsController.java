package cn.staitech.file.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2023/10/17 16:10:47
 */
@Slf4j
@RequestMapping("/statics")
@Api(value = "获取缩略图")
@RestController
public class StaticsController {
    @Value("${file.path}")
    private String baseDir;

    @ApiOperation(value = "切片管理-缩略图查询")
    @GetMapping(value = "/thumbnail/**",produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    public  byte[] getThumbImage(HttpServletRequest request) throws Exception {
        byte[] bytes = null;
        String path = request.getServletPath();
        path = path.replace("/statics",baseDir);
        File thumbImage = new File(path);
        try {
            bytes = FileUtils.readFileToByteArray(thumbImage);
        }catch (FileNotFoundException e){
            log.error("缩略图未找到：{};;{}",path,e.getMessage());
        }
        return bytes;
    }
}
