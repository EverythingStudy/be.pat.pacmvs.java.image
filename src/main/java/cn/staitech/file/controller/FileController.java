package cn.staitech.file.controller;

import java.io.File;
import java.util.List;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import cn.staitech.common.core.domain.R;
import cn.staitech.common.log.annotation.Log;
import cn.staitech.common.log.enums.BusinessType;
import cn.staitech.file.constant.ImageConstant;
import cn.staitech.file.domain.Image;
import cn.staitech.file.service.AsyncTask;
import cn.staitech.file.service.FileService;
import cn.staitech.file.service.ImageService;
import cn.staitech.file.util.FileUploadUtils;
import cn.staitech.file.vo.Chunk;
import cn.staitech.file.vo.FileInformationOutVO;
import cn.staitech.file.vo.FileInformationVO;
import cn.staitech.file.vo.UploadDelVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

/**
 * 大文件上传接口
 *
 * @author wangf
 */
@Slf4j
@RequestMapping("/bigPicture")
@Api(value = "大文件上传相关接口", tags = "大文件上传相关接口")
@RestController
public class FileController {
	/**
	 * 获取动态开关，是否校验上传文件重复
	 */
	@Value("${file.check:true}")
	private boolean check;
	@Resource
	private FileService fileService;
	@Resource
	private ImageService imageService;
	@Resource
	private AsyncTask asyncTask;

	@Value("${file.path}")
	private String localFilePath;

	/**
	 * 文件前置信息上传
	 *
	 * @param fileInformation
	 * @return
	 */
	//@RequiresPermissions("section:slices:upload")
	@ApiOperation(value = "添加文件前置信息（大小文件共用）")
	@PostMapping("/fileInformation")
	@Log(title = "上传切片", menu = "切片管理", subMenu = "切片列表", businessType = BusinessType.INSERT)
	public R<FileInformationOutVO> fileInformation(@Validated @RequestBody FileInformationVO fileInformation) throws Exception {
		try {
			// step1：文件超过3G不允许上传
			if (Long.valueOf(fileInformation.getSize()) > ImageConstant.ALLOWED_FILE_MAXSIZE) {
				return R.fail(ImageConstant.DISALLOWED_FILE_SIZE);
			}

			// step2：判断文件扩展名是否允许上传 true可上传,false不可上传
			if (FileUploadUtils.isAllowedExtension(fileInformation.getImageName())) {
				return R.fail(ImageConstant.DISALLOWED_EXTENSION);
			}

			// step3:校验文件是否重复：校验文件名、MD5是否要返回重复的文件信息？秒传。  （校验跳过逻辑删除的文件，不判断）
			if (check && !imageService.checkImageNameAndMd5(fileInformation)) {
				return R.fail(ImageConstant.IMAGE_EXISTS);
			}
			// step4：tb_image表中增加一条图像信息,初始化，存入MD5等信息 解析文件名称获取切片编号、所属专题
			return imageService.insert(fileInformation);
		}catch (Exception e){
			e.printStackTrace();
			log.error("添加文件前置信异常：{};;{}",e.getMessage(),fileInformation);
			throw e;
		}
	}


	/**
	 * 大文件上传至本地
	 *
	 * @param request ： 请求
	 * @param imageId ： 编码
	 * @param chunk   ： 切片数
	 * @param file    ： 文件
	 * @return ： 返回结果
	 */
	//@RequiresPermissions("section:slices:upload")
	@ApiOperation(value = "每一分片文件上传")
	@PostMapping("/uploadSlice")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "imageId", value = "图片Id", required = true, dataType = "Long"),
		@ApiImplicitParam(name = "chunk", value = "分片Id", required = true, dataType = "Integer"),
		@ApiImplicitParam(name = "chunkTotal", value = "分片总数", required = true, dataType = "Integer"),
		@ApiImplicitParam(name = "chunkSize", value = "分片大小", required = true, dataType = "Long"),
		@ApiImplicitParam(name = "file", value = "分片文件", required = true, dataType = "file")})
	public R<String> uploadSlice(
			@Validated HttpServletRequest request,
			@RequestParam("imageId") Long imageId,
			@RequestParam("chunk") Integer chunk,
			@RequestParam("chunkTotal") Integer chunkTotal,
			@RequestParam("chunkSize") Long chunkSize,
			@RequestParam("file") MultipartFile file)
					throws Exception {

		Chunk chunkObj = new Chunk()
				.setChunkNumber(chunk)
				.setFile(file)
				.setFilename(file.getName())
				.setTotalChunks(chunkTotal)
				.setImageId(imageId)
				.setChunkSize(chunkSize);

		if (fileService.mergeChunk(chunkObj)) {
			return R.ok(ImageConstant.FILE_SLIDE_UPLOAD_SUCCESS);
		} else {
			return R.fail(ImageConstant.FILE_SLIDE_UPLOAD_FAILURE);
		}
	}

	/**
	 * 删除文件
	 *
	 * @param req UploadDelVO
	 * @return
	 * @throws InterruptedException
	 */
	// @RequiresPermissions("anno:image:del")
	@ApiOperation(value = "文件上传失败释放资源")
	@PostMapping("/uploadDel")
	public R<String> uploadDel(@RequestBody UploadDelVO req) throws InterruptedException {
		Long[] ids = req.getIds();
		QueryWrapper<Image> queryWrapper = Wrappers.query();
		queryWrapper.in("image_id",ids);
		queryWrapper.eq("archive_status",ImageConstant.SLIDE_FILE_UNBACK);
		List<Image> imageList = imageService.list(queryWrapper);
		for (Image image : imageList) {
			String imagePath = image.getImagePath();
			File imageFile = new File(imagePath);
			if (imageFile.exists()){
				imageFile.delete();
			}
			String tempPath = localFilePath + "/LargePictureSlice/" + image.getImageId();
			// 异步删除MD5临时文件
			asyncTask.deleteFileTask(new File(tempPath));
			// 物理MySQL表删除记录
			imageService.removeById(image.getImageId());
		}
		return R.ok("全部资源释放成功");
	}

	/**
	 * 删除文件
	 *
	 * @param req UploadDelVO
	 * @return
	 * @throws InterruptedException
	 */
	// @RequiresPermissions("anno:image:del")
	@ApiOperation(value = "切片管理-删除切片")
	@PostMapping("/deleteSlide")
	public R deleteSlide(@RequestBody UploadDelVO req) throws InterruptedException {
		Long[] ids = req.getIds();
		QueryWrapper<Image> queryWrapper = Wrappers.query();
		queryWrapper.in("image_id",ids);
		queryWrapper.eq("archive_status",ImageConstant.SLIDE_FILE_UNBACK);
		List<Image> imageList = imageService.list(queryWrapper);
		for (Image image : imageList) {
			String imagePath = image.getImagePath();
			File imageFile = new File(imagePath);
			if (imageFile.exists()){
				imageFile.delete();
			}
			String tempPath = localFilePath + "/LargePictureSlice/" + image.getImageId();
			// 异步删除MD5临时文件
			asyncTask.deleteFileTask(new File(tempPath));
			// 物理MySQL表删除记录
			imageService.removeById(image.getImageId());
		}
		return R.ok();
	}

}
