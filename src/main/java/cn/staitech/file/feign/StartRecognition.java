package cn.staitech.file.feign;

import cn.staitech.common.log.enums.BusinessType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
public class StartRecognition {
	@ApiModelProperty(value = "图片ID")
	@TableId(value = "image_id", type = IdType.AUTO)
	private Long imageId;
	@ApiModelProperty(value = "切片编号-文件名称")
	private String imageName;
	@ApiModelProperty(value = "WSI图片绝对路径")
	private String imagePath;
	@ApiModelProperty(value = "原图片绝对路径")
	private String imageUrl;
	@ApiModelProperty(value = "缩略图")
	private String thumbUrl;
	@ApiModelProperty(value = "大小")
	private String size;
	@ApiModelProperty(value = "全局大小")
	private String globalSize;
	@ApiModelProperty(value = "分辨率")
	private String resolvingPower;

	@ApiModelProperty(value = "创建人ID")
	private Long createBy;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@ApiModelProperty(value = "创建时间 ")
	private Date createTime;
	@ApiModelProperty(value = "更新人ID")
	private Long updateBy;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")

	@ApiModelProperty(value = "切片编号-UUID")
	private String imageCode;
	@ApiModelProperty(value = "所属专题", hidden = true)
	private Long topicId;
	@ApiModelProperty(value = "所属专题-专题名称")
	private String topicName;
	@ApiModelProperty(value = "切片状态：0上传中、1上传失败、2解析中、3解析失败、4可用")
	private short status;
	@ApiModelProperty(value = "所在主机编号")
	private short hostId;

	private BusinessType businessType;
	@ApiModelProperty(value = "机构编号")
	@TableField(value = "organization_id")
	private Long organizationId;
	@ApiModelProperty(value = "轮次ID-1到10")
	private Long roundId;

	@ApiModelProperty(value = "业务类型:1原始切片（默认）、2预测切片")
	private Integer bizType;

	@ApiModelProperty(value = "图像来源、上传方式(1前端上传，2目录选片，3TCP客户端上传)")
	private Integer source;
	@TableField(value = "file_name")
	@ApiModelProperty(value = "无扩展名文件名称")
	private String fileName;
	@ApiModelProperty(value = "算法名称")
	private String algorithm_name;

	@ApiModelProperty(value = "机构名称")
	@TableField(exist = false)
	private String organizationName;

	//	@ApiModelProperty(value = "轮次名称")
	//	@TableField(exist = false)
	//	private String roundName;
	//  @ApiModelProperty(value = "每层的切片个数")
	//    private String tileCountList;
	//    @ApiModelProperty(value = "总level数")
	//    private Integer levelCount;
	//    @ApiModelProperty(value = "前端上传时总切片个数")
	//    private Integer chunkTotal;
	//    @ApiModelProperty(value = "md5摘要")
	//    private String md5;
	//    @ApiModelProperty(value = "x轴分辨率")
	//    private String resolutionX;
	//    @ApiModelProperty(value = "y轴分辨率")
	//    private String resolutionY;
	//    @ApiModelProperty(value = "原放大倍数")
	//    private Integer sourceLens;
	//    @ApiModelProperty(value = "Macro图")
	//    private String macroUrl;
	//    @ApiModelProperty(value = "Label图")
	//    private String labelUrl;
	//    @ApiModelProperty(value = "1024缩略图路径（用于缓存、标注缩略图时需要）")
	//    private String cacheUrl;
	//    @ApiModelProperty(value = "原图缩到cache图的倍数")
	//    private String multiple;
	//    @ApiModelProperty(value = "文件格式")
	//    private String format;
	//    @ApiModelProperty(value = "宽度")
	//    private String width;
	//    @ApiModelProperty(value = "高度")
	//    private String height;
	//    @ApiModelProperty(value = "深度")
	//    private String depth;
	//  @ApiModelProperty(value = "业务类型名称:1原始切片（默认）、2预测切片")
	//    @TableField(exist = false)
	//    private String businessTypeName;
	//  @ApiModelProperty(value = "业务类型")
	//    @TableField(exist = false)
	//    @ApiModelProperty(value = "更新时间 ")
	//    private Date updateTime;
}

