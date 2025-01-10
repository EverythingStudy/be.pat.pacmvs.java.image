package cn.staitech.file.util;

import java.io.IOException;

/**
 * @author wangf
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

    public static String getThumbPathKey(String configKey) {
        return THUMB_PATH_IMAGE_KEY + configKey;
    }

    public static String getMultipleKey(String configKey) {
        return MULTIPLE_KEY + configKey;
    }


    /**
     * image Hashcode <hashcode,imageId>
     *
     * @param configKey
     * @return
     */
    /*public static String getRemotefileHashKey(BusinessType businessType, String configKey) {
        return REMOTEFILE_HASH_KEY + businessType.toString() + ":" + configKey.hashCode();
    }*/

    /**
     * jpeg图片转tiff
     *
     * @param source
     * @param target
     * @throws IOException
     * @throws InterruptedException
     */
    public static void convertToPyramidalTIFF(String source, String target) throws IOException, InterruptedException {
        String vipsExecutable = "vips ";
        // String vipsExecutable = "D:\\d\\vips\\vips\\vips-dev-w64-web-8.13.0\\vips-dev-8.13\\bin\\vips.exe ";
        String compression = "lzw";//jpeg -Q 95
        String tileSize = "256";

        String command = vipsExecutable + " tiffsave " + source + " " + target + " --bigtiff " +
                "--tile "
                + "--tile-width " + tileSize + " --tile-height " + tileSize + " --pyramid --compression " + compression;
        System.out.println("jpeg图片转tiff command = " + command);
        Process exec = Runtime.getRuntime().exec(command);
        exec.waitFor();
    }

}
