package cn.staitech.file.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


@Data
public class SpecialAnnDataVO {

    @ApiModelProperty(required = true, value = "标注id")
    private Long annotationId;

    @ApiModelProperty(required = true, value = "输出目录")
    private String outPath;

    @ApiModelProperty(required = true, value = "msg")
    private String msg;
}
