package cn.staitech.file.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openslide.OpenSlide;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2023/11/9 10:38:07
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImageConversionsionResp {
    private OpenSlide openSlide;
    private String destPath;
}
