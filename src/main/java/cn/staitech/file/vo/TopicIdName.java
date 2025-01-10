package cn.staitech.file.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author: wangfeng
 * @create: 2023-09-15 16:12:40
 * @Description: 专题列表
 */
@Data
public class TopicIdName implements Serializable {

    /**
     * 专题ID
     */
    @ApiModelProperty(value = "专题ID")
    private Long topicId;


    /**
     * 专题名称
     */
    @ApiModelProperty(value = "专题名称")
    private String topicName;
}
