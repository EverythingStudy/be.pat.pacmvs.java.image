package cn.staitech.file.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 图像上传前置信息
 *
 * @author admin
 */
@Data
public class FileInformationVO {
    @ApiModelProperty(value = "图片Id")
    private Long imageId;

    @ApiModelProperty(value = "图片名称", required = true)
    @NotBlank(message = "文件名为必传，且不能为空字符串")
    @Size(min = 1, max = 100, message = "文件名称长度不能超过100个字符")
    private String imageName;

    @ApiModelProperty(value = "md5", required = true)
    @NotBlank(message = "md5为必传，且不能为空字符串")
    private String md5;

    @ApiModelProperty(value = "分片个数")
    private Integer chunkTotal;

    @ApiModelProperty(value = "图片大小", required = true)
    @NotBlank(message = "size为必传，且不能为空字符串")
    private String size;

    @ApiModelProperty(value = "机构ID", required = true)
    @NotNull(message = "机构ID不能为空")
    private Long organizationId;

    @ApiModelProperty(value = "专题名称")
    @NotBlank(message = "专题名称不能为空")
    private String topicName;

    @ApiModelProperty(value = "项目分类ID")
    private Integer projectTypeId;
    @ApiModelProperty(value = "uuid")
    private String uuid;
    @ApiModelProperty(value = "userId")
    private Long userId;
}
