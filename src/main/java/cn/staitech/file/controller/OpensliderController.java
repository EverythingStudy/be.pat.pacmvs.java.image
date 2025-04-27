package cn.staitech.file.controller;

import cn.staitech.common.core.domain.R;
import cn.staitech.file.service.AsyncTask;
import cn.staitech.file.service.OpenSlideService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;
import java.io.File;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2025/4/22 09:32:40
 */
@RestController
@RequestMapping("/smallPicture")
@Api(value = "小文件上传相关接口", tags = "小文件上传相关接口")
public class OpensliderController {
    private static final Logger log = LoggerFactory.getLogger(OpensliderController.class);

    @Resource
    private OpenSlideService openSlideService;
    @Resource
    private AsyncTask asyncTask;

    /**
     * 文件上传请求
     */
    @ApiOperation(value = "小文件上传接口")
    @PostMapping("/upload")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "file", value = "小文件", required = true, dataType = "file"),
            @ApiImplicitParam(name = "imageId", value = "图片Id", required = true, dataType = "Long")
    })
    public R<String> upload(@RequestParam("file") MultipartFile file, @RequestParam(value = "imageId") Long imageId) {
        try {
            // 上传并返回绝对路径地址
            String absoluteFile = openSlideService.uploadFile(file, imageId);
            // 异步生成缩略图
            asyncTask.processThumbTask(new File(absoluteFile), imageId);
            log.info("小文件上传成功: imageId:{} ,absoluteFile:{}", imageId, absoluteFile);
            return R.ok(absoluteFile);
        } catch (Exception e) {
            log.info("小文件上传失败: imageId:{} ,e: {}", imageId, e);
            return R.fail(e.getMessage());
        }
    }

}