package cn.staitech.file.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class PathVO implements Serializable {

    private String path;
    private String filter;
}
