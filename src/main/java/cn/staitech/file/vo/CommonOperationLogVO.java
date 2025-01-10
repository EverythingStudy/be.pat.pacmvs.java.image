package cn.staitech.file.vo;

import java.util.List;

import com.baomidou.mybatisplus.annotation.TableField;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class CommonOperationLogVO{
	
	@ApiModelProperty(value = "按钮名称")
	@TableField(exist = false)
	private String buttonName;

	@ApiModelProperty(value = "模块页面")
	@TableField(exist = false)
	private String modulePageName;

	@ApiModelProperty(value = "修改原因")
	@TableField(exist = false)
	private String modifyReason;
	
	@ApiModelProperty(value = "处理人名字")
	@TableField(exist = false)
	private String createUserName;
	
	@ApiModelProperty(value = "处理人")
	@TableField(exist = false)
	private Long createUserId;
	
	@ApiModelProperty(value = "ipUrl")
	@TableField(exist = false)
	private String ipUrl;
	
	@ApiModelProperty(value = "页签")
	@TableField(exist = false)
	private Integer pageValue;


	@ApiModelProperty(value = "修改信息")
	@TableField(exist = false)
	private List<ColumnChild> modifyList;

	//内部类，代表列的子项
	@Data
	public static class ColumnChild {
		private String beforeVal;
		private String afterVal;
		private String field;
		private String frontColumnName;
	}
}
