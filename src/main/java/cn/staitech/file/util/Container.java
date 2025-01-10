package cn.staitech.file.util;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ConcurrentHashMap
 * @author wangf
 */
public class Container {
    
    // 定义一个基于多线程 的 hashmap
    public static final Map<Long, ArrayList<Integer>> FILE_MAP = new ConcurrentHashMap<>();
    public static final Map<Long, AtomicInteger> FILE_MAP_SYN = new ConcurrentHashMap<>();
}