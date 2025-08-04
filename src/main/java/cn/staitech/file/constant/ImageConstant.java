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
     * 图像状态：0-上传中；1-上传失败；2-解析中；3-解析失败；4-可用；5-信息解析中；6-信息解析失败；7-处理中；8-处理失败
     */
    public static final String IMAGE_STATUS_TILE_PROCESS_FAIL = "8";
    public static final String IMAGE_STATUS_TILE_PROCESSING = "7";
    public static final String IMAGE_STATUS_MSG_PARSE_FAIL = "6";
    public static final String IMAGE_STATUS_MSG_PARSING = "5";
    public static final String IMAGE_STATUS_ENABLE = "4";
    public static final String IMAGE_STATUS_PARSE_FAIL = "3";
    public static final String IMAGE_STATUS_PARSING = "2";
    public static final String IMAGE_STATUS_UPLOAD_FAIL = "1";
    public static final String IMAGE_STATUS_UPLOADING = "0";

    /**
     * 切片存储目录
     */
    public static final String SLIDE_STORAGE_RETRY = "Retry";
    public static final String SLIDE_STORAGE_FAILED = "Failed";
    public static final String SLIDE_STORAGE = "Slides";
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
