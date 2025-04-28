package cn.staitech.file.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import javax.validation.constraints.NotNull;


/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2025/4/22 09:32:40
 */
@Data
public class FileInsertVO {
    @ApiModelProperty(value = "图片文件绝对路径数组", required = true)
    private String[] fileList;
    @ApiModelProperty(value = "机构ID", required = true)
    @NotNull(message = "{FileInformationVO.organizationId.isnull}")
    private Long organizationId;
    @ApiModelProperty(value = "轮次ID")
    private Long roundId;
    @ApiModelProperty(value = "业务类型:1原始切片（默认）、2预测切片", hidden = true)
    private Integer bizType;
}
