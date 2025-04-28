package cn.staitech.file.controller;

import cn.staitech.common.core.domain.R;
import cn.staitech.file.domain.Image;
import cn.staitech.file.service.ImageService;
import cn.staitech.file.service.OpenSlideService;
import cn.staitech.file.vo.FileInsertVO;
import com.sun.org.apache.bcel.internal.generic.I2F;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.List;

/**
 * @author mugw
 * @version 1.0
 * @description 服务器选片
 * @date 2025/4/22 09:32:40
 */
@Slf4j
@RequestMapping("/image")
@Api(value = "服务器选片")
@RestController
public class ImageController {
    @Resource
    private OpenSlideService openSlideService;
    @Resource
    private ImageService imageService;

    /**
     * 服务器选片
     * @param vo
     * @return
     * @throws Exception
     */
    @ApiOperation(value = "服务器选片")
    @PostMapping("/add")
    public R add(@Validated @RequestBody FileInsertVO vo) throws Exception {
        List<Image> images = imageService.batchFileHandle(vo);
        openSlideService.processThumb(images);
        return R.ok();
    }

    @ApiOperation(value = "重新解析所有失败数据")
    @PostMapping("/reparse")
    public R reparse(@RequestBody List<Long> imageIds) throws Exception {
        openSlideService.reparse(imageIds);
        return R.ok();
    }

    @ApiOperation(value = "获取原始切片根目录")
    @GetMapping("/getFilePath")
    public R getFilePath(@Value("${file.path}") String filePath) throws Exception {
        return R.ok(filePath);
    }
}
