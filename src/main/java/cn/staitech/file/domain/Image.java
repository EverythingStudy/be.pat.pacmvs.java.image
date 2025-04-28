package cn.staitech.file.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
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
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@TableName(value = "tb_image")
public class Image implements Serializable {
    /**
     * 图像ID
     */
    @TableId(type = IdType.AUTO)
    private Long imageId;

    /**
     * 无扩展名文件名称
     */
    private String fileName;

    /**
     * 图像名称（文件名）
     */
    private String imageName;

    /**
     * 图像的绝对路径
     */
    private String imagePath;

    /**
     * 图像URL地址
     */
    private String imageUrl;

    /**
     * 缩略图URL地址
     */
    private String thumbUrl;

    /**
     * macro图片URL地址
     */
    private String macroUrl;

    /**
     * label图片RUL地址
     */
    private String labelUrl;

    /**
     * 1024缩略图路径（用于缓存、标注缩略图时需要）
     */
    private String cacheUrl;

    /**
     * 原图缩到cache图的倍数
     */
    private String multiple;

    /**
     * 文件格式
     */
    private String format;

    /**
     * 宽度
     */
    private String width;

    /**
     * 高度
     */
    private String height;

    /**
     * 深度
     */
    private String depth;

    /**
     * 大小
     */
    private String size;

    /**
     * 全局大小
     */
    private String globalSize;

    /**
     * 分辨率
     */
    private String resolvingPower;

    /**
     * 每层的切片个数
     */
    private String tileCountList;

    /**
     * 总层数（小于2则失败）
     */
    private Integer levelCount;

    /**
     * 前端总切片个数
     */
    private Integer chunkTotal;

    /**
     * 图片的MD5值
     */
    private String md5;

    /**
     * x轴分辨率
     */
    private String resolutionX;

    /**
     * y轴分辨率
     */
    private String resolutionY;

    /**
     * 原放大倍数
     */
    private Integer sourceLens;

    /**
     * 处理状态，不可用原因共三种，0上传失败（MD5校验不通过），1解析中，2解析失败（不能获得缩略图）（1.0：0上传未合并,1合并且生成缩略图,2传输图像）
     */
    private String processFlag;

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
     * 更新时间
     */
    private Date updateTime;

    /**
     * 图片（切片）编号
     */
    private String imageCode;

    /**
     * 专题ID
     */
    private Long topicId;

    @ApiModelProperty("专题编号")
    @TableField(value = "topic_name")
    private String topicName;

    /**
     * 是否可用0不可用1可用
     */
    private String status;

    /**
     * 业务类型（1原始切片（默认）、2预测切片）
     */
    private Integer bizType;

    /**
     * 图像来源(1手动上传，2服务器读取)
     */
    private Integer source;

    private Long organizationId;

    @ApiModelProperty("动物号")
    private String animalCode;

    @ApiModelProperty("蜡块号")
    private String waxCode;

    @ApiModelProperty("组别号")
    private String groupCode;

    @ApiModelProperty("性别（M；F）")
    private String sexFlag;

    @ApiModelProperty("文件名解析状态（0失败1成功）")
    private Integer analyzeStatus;

}