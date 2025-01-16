package cn.staitech.file.controller;

import cn.staitech.common.core.domain.R;
import cn.staitech.file.constant.ImageConstant;
import cn.staitech.file.service.OpenSlideService;
import cn.staitech.file.vo.FileInsertVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 大文件上传接口
 *
 * @author wangf
 */
@Slf4j
@RequestMapping("/image")
@Api(value = "切片管理-原始切片-选择切片", tags = "切片管理-原始切片-选择切片")
@RestController
public class ImageController {
    @Resource
    private OpenSlideService openSlideService;

    /**
     * 切片管理-预测图片-选择切片
     *
     * @param vo
     * @return
     * @throws Exception
     */
    @ApiOperation(value = "选择切片")
    @PostMapping("/add")
    public R add(@Validated @RequestBody FileInsertVO vo) throws Exception {

        if (openSlideService.getCache().getIfPresent(vo.getTopicName()) != null) {
            return R.fail(ImageConstant.SERVER_IMAGE_UPLOAD_FAILURE1);
        }
        vo.setBizType(1);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(new ReparseImageTask(executorService, vo));
        return R.ok();
    }

    class ReparseImageTask implements Runnable {

        private ExecutorService executorService;
        private FileInsertVO vo;

        public ReparseImageTask(ExecutorService executorService, FileInsertVO vo) {
            this.executorService = executorService;
            this.vo = vo;
        }
        @Override
        public void run() {
            try {
                long start = System.currentTimeMillis();
                openSlideService.asynSaveBatch(vo);
                long time = System.currentTimeMillis()-start;
                log.info("异步批量服务器读取切片耗时：[{}]",time);
            } catch (Exception e) {
                log.error("服务器选片异常:[{}]", e.getMessage());
            }
        }
    }

    @ApiOperation(value = "原始切片-选择切片-查询处理中的原始切片数据")
    @PostMapping(value = "/queryProcessUploadTopic")
    public R queryProcessUploadTopic() throws Exception {
        return R.ok(openSlideService.getCache());
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
