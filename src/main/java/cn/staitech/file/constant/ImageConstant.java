package cn.staitech.file.constant;

/**
 * @author 张争
 * @date 2022/9/29 11:32
 */

public class ImageConstant {
    public static final String SVS = "svs";
    public static final String NDPI = "ndpi";

    public static final String IMAGE_EXISTS = "该文件已经存在，请重新上传";

    public static final String DISALLOWED_EXTENSION = "上传的图像格式暂不支持";
    public static final String DISALLOWED_FILE_SIZE = "不允许上传5G以上的文件";
    public static final String FILE_SLIDE_UPLOAD_SUCCESS = "文件分片上传成功";
    public static final String FILE_SLIDE_UPLOAD_FAILURE = "文件分片上传失败";
    public static final String SERVER_IMAGE_UPLOAD_FAILURE = "服务器上传电子切片专题信息已归档";
    public static final String SERVER_IMAGE_UPLOAD_FAILURE1 = "正在解析该专题图像，不可重复上传";
    public static final String IMAGE_STATUS_ENABLE = "4";
    public static final String IMAGE_STATUS_UNABLE = "1";
    public static final String IMAGE_PROCESS_PARSING = "1";
    public static final String IMAGE_PROCESS_PARSE_SUCCESS = "3";
    public static final String IMAGE_PROCESS_PARSE_FAIL = "2";
    public static final String IMAGE_PROCESS_UPLOAD_FAIL = "0";
    public static final String IMAGE_PROCESS_UPLOADING = "5";
    public static final Integer IMAGE_SOURCE_UPLOAD = 1;
    public static final Integer IMAGE_SOURCE_SERVER = 2;

    public static final String  SLIDE_FILE_UNBACK  = "1";
    public static final String  SLIDE_FILE_BACK  = "2";
    public static final String  ARCHIVE_STATUS_1  = "1";
    public static final String  ARCHIVE_STATUS_2  = "2";

    /**
     * 文件超过3G不允许上传
     */
    //public static final Long ALLOWED_FILE_MAXSIZE = 1024 * 1024 * 1024 * 3L;
    public static final Long ALLOWED_FILE_MAXSIZE = 1024 * 1024 * 1024 * 5L;
    public static final String THUMB_BASE_DIR = "/file/statics";


}
