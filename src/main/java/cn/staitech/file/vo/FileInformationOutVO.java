package cn.staitech.file.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author admin
 */
@Data
public class FileInformationOutVO {

    @ApiModelProperty(value = "图片Id", required = true)
    private Long imageId;

    @ApiModelProperty(value = "主机Id", required = true)
    private Short hostId;
}
