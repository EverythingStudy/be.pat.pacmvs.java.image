package cn.staitech.file.util;

import java.io.IOException;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2025/4/22 09:32:40
 */
public class ImageUtils {

    /**
     * 图片管理 image key
     */
    public static final String PATH_IMAGE_KEY = "path_image:";
    public static final String PATH_IMAGE_KEY_ALL = "path_image:*";
    public static final String THUMB_PATH_IMAGE_KEY = "thumb_path_image:";
    public static final String MULTIPLE_KEY = "multiple_image:";
    public static final String REMOTEFILE_HASH_KEY = "REMOTEFILE_";

    /**
     * 设置cache key
     *
     * @param configKey 参数键
     * @return 缓存键key
     */
    public static String getPathKey(String configKey) {
        return PATH_IMAGE_KEY + configKey;
    }

}
