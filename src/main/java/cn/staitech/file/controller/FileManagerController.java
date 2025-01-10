package cn.staitech.file.controller;

import cn.staitech.common.core.domain.R;
import cn.staitech.file.vo.FileNode;
import cn.staitech.file.vo.PathVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author admin
 */
@Slf4j
@Api(value = "文件管理器", tags = "文件管理器")
@RestController
@RequestMapping("/filemanager")
public class FileManagerController {
    @Value("${file.path}")
    private String baseDir;
    //private String baseDir = File.separator+"home"+File.separator+"staitech"+File.separator+"Slides";

    /**
     * /home/pat_saas/Data
     * /home/pat_saas/Slides
     * /home/pat_saas/Upload
     */

    /**
     * 查询目录下的文件夹和文件列表
     *
     * @param vo
     * @return
     */
    @ApiOperation(value = "切片上传-查询目录下的文件夹和文件列表")
    @PostMapping(value = "/list")
    public R<List<FileNode>> list(@RequestBody PathVO vo) {
        String path = vo.getPath();

        if (!path.startsWith(baseDir)) {
            path = baseDir;
        }

        File file = new File(path);

        // 如果传入的参数不存在或是文件返回提示
        if (!file.exists()) {
            return R.fail("路径不存在");
        }

        if (file.isFile()) {
            return R.fail("是文件，不是文件夹");
        }

        List<FileNode> fileNodeList = new ArrayList<>();

        if (file.isDirectory()) {
            File[] fileArray = file.listFiles();
            for (File f : fileArray) {
                if ((baseDir+File.separator+"cacheThumbnail").equals(f.getAbsolutePath())||(baseDir+File.separator+"label").equals(f.getAbsolutePath())||
                        (baseDir+File.separator+"macro").equals(f.getAbsolutePath())||(baseDir+File.separator+"thumbnail").equals(f.getAbsolutePath())) {
                    continue;
                }
                String type = f.isDirectory() ? "dir" : "file";
                if ("file".equals(type)&& StringUtils.isNotEmpty(vo.getFilter())&&!f.getName().endsWith(vo.getFilter())){
                    continue;
                }
                FileNode node = new FileNode(f.getName(), convertWindowsToLinuxPath(f.getAbsolutePath()), type, f.length());
                fileNodeList.add(node);
            }
        }

        // 排序
        List<FileNode> nodes = fileNodeList.stream().
                sorted(Comparator.comparing(FileNode::getType).
                        thenComparing(FileNode::getType, Comparator.reverseOrder())).collect(Collectors.toList());

        return R.ok(nodes, "成功");
    }

    /**
     * 将 Windows 文件路径转换为 Linux 文件路径
     *
     * @param windowsPath Windows 文件路径
     * @return Linux 文件路径
     */
    public static String convertWindowsToLinuxPath(String windowsPath) {
        if (windowsPath == null || windowsPath.isEmpty()) {
            return windowsPath;
        }

        // 替换反斜杠为正斜杠
        String linuxPath = windowsPath.replace('\\', '/');

        // 处理驱动器盘符，假设 C:\ 映射到 /mnt/c/
        if (linuxPath.startsWith("/")) {
            // 如果已经是 Linux 路径，直接返回
            return linuxPath;
        } else if (linuxPath.length() >= 3 && linuxPath.charAt(1) == ':') {
            // 处理驱动器盘符
            char driveLetter = Character.toLowerCase(linuxPath.charAt(0));
            //linuxPath = "/mnt/" + driveLetter + "/" + linuxPath.substring(3);
            linuxPath = "/"+linuxPath.substring(3);
        }

        return linuxPath;
    }
}
