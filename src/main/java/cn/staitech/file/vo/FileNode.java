package cn.staitech.file.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileNode {
    private String name;
    private String path;
    private String type;
    private Long size;


}
