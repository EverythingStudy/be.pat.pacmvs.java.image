package cn.staitech.file.controller;

import cn.staitech.common.core.domain.R;
import cn.staitech.common.security.utils.SecurityUtils;
import cn.staitech.file.domain.Image;
import cn.staitech.file.service.ImageService;
import cn.staitech.file.service.OpenSlideService;
import cn.staitech.file.vo.FileInsertVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.List;

import static cn.staitech.file.constant.ImageConstant.IMAGE_NAME_PARSE_FAIL;
import static cn.staitech.file.constant.ImageConstant.IMAGE_PROCESS_PARSE_SUCCESS;

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
    @ApiOperation(value = "检查是否存在解析失败的切片", tags =  {"V2.6.0"})
    @GetMapping("/checkFailImage")
    public R checkFailImage() throws Exception {
        long count = imageService.count(Wrappers.<Image>lambdaQuery().and(w -> w.ne(Image::getStatus, IMAGE_PROCESS_PARSE_SUCCESS).or()
                        .eq(Image::getAnalyzeStatus,IMAGE_NAME_PARSE_FAIL))
                .eq(Image::getOrganizationId, SecurityUtils.getOrganizationId()));
        return R.ok(count>0);
    }


}
