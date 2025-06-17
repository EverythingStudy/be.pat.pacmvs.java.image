package cn.staitech.file.constant;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2025/4/22 09:32:40
 */
public class ImageConstant {
    public static final String SVS = "svs";
    public static final String NDPI = "ndpi";

    public static final String IMAGE_EXISTS = "该文件已经存在，请重新上传";

    public static final String DISALLOWED_EXTENSION = "上传的图像格式暂不支持";
    public static final String DISALLOWED_FILE_SIZE = "不允许上传5G以上的文件";
    public static final String FILE_SLIDE_UPLOAD_SUCCESS = "文件分片上传成功";
    public static final String FILE_SLIDE_UPLOAD_FAILURE = "文件分片上传失败";

    /**
     * 图像解析状态
     */
    public static final String IMAGE_STATUS_ENABLE = "4";
    public static final String IMAGE_STATUS_UNABLE = "1";
    public static final String IMAGE_PROCESS_PARSING = "2";
    public static final String IMAGE_PROCESS_PARSE_SUCCESS = "4";
    public static final String IMAGE_PROCESS_PARSE_FAIL = "3";
    public static final String IMAGE_PROCESS_UPLOAD_FAIL = "1";
    public static final String IMAGE_PROCESS_UPLOADING = "0";

    /**
     * 图像来源
     */
    public static final Integer IMAGE_SOURCE_UPLOAD = 1;
    public static final Integer IMAGE_SOURCE_SERVER = 2;

    /**
     * 图像名称解析状态
     */
    public static final Integer IMAGE_NAME_PARSE_FAIL = 0;
    public static final Integer IMAGE_NAME_PARSE_SUCC = 1;


    public static final Long ALLOWED_FILE_MAXSIZE = 1024 * 1024 * 1024 * 5L;
    public static final String THUMB_BASE_DIR = "/file/statics";


    //解剖期限常量：TN、RN、DOS(FD)、DOS(MOR)、DOS（FD）、DOS（MOR）
    public static final String[] ANATOMY_PERIOD_CONSTANT = {"TN", "RN", "DOS(FD)", "DOS(MOR)", "DOS（FD）", "DOS（MOR）"};

    //雌性
    public static final String FEMALE = "F";
    //雄性
    public static final String MALE = "M";



}
