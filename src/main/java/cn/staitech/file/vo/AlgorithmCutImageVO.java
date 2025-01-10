package cn.staitech.file.vo;


import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AlgorithmCutImageVO {
    
    
    /**
     * imageId
     */
    @ApiModelProperty(value = "imageId")
    private Long imageId ;
    
    
    /**
     * specialImageId
     */
    @ApiModelProperty(value = "specialImageId")
    private Long specialImageId ;
    
    @ApiModelProperty(value = "clientId")
    private Integer clientId ;
    
    @ApiModelProperty(value = "clientIp")
    private String clientIp ;
    
    
    
    /**
     * 标注结果列表
     */
    @ApiModelProperty(value = "标注结果列表")
    private List<SpecialAnnDataVO> annoData  = new ArrayList<>();
    
    

}
