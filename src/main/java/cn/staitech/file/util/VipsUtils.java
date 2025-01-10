package cn.staitech.file.util;

import java.io.IOException;

/**
 * @author: wangfeng
 * @create: 2023-07-24 16:03:21
 * @Description: LibVips图像格式转换
 */

public class VipsUtils {
    /**
     * jpeg图片转tiff
     *
     * 静态方法是属于类的而不属于对象的。同样的，synchronized修饰的静态方法锁定的是这个类的所有对象。
     *
     * @param source
     * @param target
     * @throws IOException
     * @throws InterruptedException
     */
    public static boolean convertToPyramidalTIFF(String source, String target) throws IOException, InterruptedException {
        String vipsExecutable = "vips ";
        // String vipsExecutable = "D:\\d\\vips\\vips\\vips-dev-w64-web-8.13.0\\vips-dev-8.13\\bin\\vips.exe ";
        String compression = "lzw"; // jpeg -Q 95
        String tileSize = "256";

        String command = vipsExecutable + " tiffsave " + source + " " + target + " --bigtiff " +
                "--tile "
                + "--tile-width " + tileSize + " --tile-height " + tileSize + " --pyramid --compression " + compression;

        System.out.println("jpeg图片转tiff command = " + command);
        Process exec = Runtime.getRuntime().exec(command);
        exec.waitFor();
        return true;
    }
}
