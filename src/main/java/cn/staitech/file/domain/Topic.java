package cn.staitech.file.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.Date;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2025/4/22 09:32:40
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
