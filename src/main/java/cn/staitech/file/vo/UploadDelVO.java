package cn.staitech.file.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class UploadDelVO extends CommonOperationLogVO {
    @ApiModelProperty(value = "要释放的Id列表", required = true)
    private Long[] ids;
}
