package cn.staitech.file.domain;

/**
 * @author: wangfeng
 * @create: 2023-06-08 13:37:45
 * @Description: Topic
 */


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

/**
 * 切片-专题（原图像）表 tb_topic
 *
 * @author WangFeng
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "tb_topic")
public class Topic implements Serializable {

    /**
     * 主键id
     */
    @TableId(type = IdType.AUTO)
    private Long topicId;

    /**
     * 专题名称（唯一约束）
     */
    @TableId
    private String topicName;

    /**
     * 创建者
     */
    private Long createBy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新者
     */
    private Long updateBy;

    /**
     * 项目类型ID .
     */
    @TableField(value = "project_type_id")
    private Integer projectTypeId;

    /**
     * 更新时间
     */
    private Date updateTime;

    private Long organizationId;



}
