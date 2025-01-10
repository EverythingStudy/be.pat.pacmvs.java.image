package cn.staitech.file.mapper;

import cn.staitech.file.domain.Image;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author muguowei
 */
@Repository
public interface ImageMapper extends BaseMapper<Image> {

    /**
     * 根据主键更新process_flag字段
     * @param record
     * @return
     */
    int updateByPrimaryKeyToProcessFlag(Image record);

}