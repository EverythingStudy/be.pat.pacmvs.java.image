package cn.staitech.file.mapper;

import cn.staitech.file.domain.Image;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2025/4/22 09:32:40
 */
@Repository
public interface ImageMapper extends BaseMapper<Image> {

}