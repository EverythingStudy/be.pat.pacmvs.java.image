package cn.staitech.file.vo.image;

import cn.staitech.sft.logaudit.req.LogAuditBaseReq;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;


/**
 * @author mgw
 * @version 1.0
 * @since 2025/12/31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageLogDetailReq extends LogAuditBaseReq {

    @ApiModelProperty(value = "图像id", hidden = true)
    private Long imageId;

    @ApiModelProperty("专题名称")
    private String topicName;

    @ApiModelProperty("切片编号")
    private String imageName;

    @ApiModelProperty("图像大小")
    private String size;

    @ApiModelProperty("机构ID")
    private Long organizationId;

    @ApiModelProperty("上传时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @ApiModelProperty("状态（0-上传中；1-上传失败；2-解析中；3-解析失败；4-可用；5-信息解析中；6-信息解析失败；7-处理中；8-处理失败）")
    private Integer status;

    @ApiModelProperty("信息解析状态（0失败1成功）")
    private Integer analyzeStatus;

    @Schema(description = "返回数据加密信息")
    private String encrypt;
}
