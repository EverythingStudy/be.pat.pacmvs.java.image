import sys
import os
import time
import openslide
from openslide import OpenSlideCache
import numpy as np
from PIL import Image,ImageCms
from openslide.deepzoom import DeepZoomGenerator
from deepzoom_custom import DeepZoomGeneratorCustom
import concurrent.futures
from concurrent.futures import ThreadPoolExecutor
from queue import Queue
import threading

TILE_SIZE = 512

def cunrrent_output_zoomify_tiles(slide, output_dir):
    """
    创建 Zoomify 格式的瓦片（流式处理，限制内存使用）
    :param slide: OpenSlide 对象
    :param output_dir: 输出目录
    """
    start_time = time.time()

    # 创建输出目录
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    dz = DeepZoomGeneratorCustom(slide, TILE_SIZE, 0, True)
    dz_level_count = dz.level_count

    # 创建线程池
    max_workers = os.cpu_count()*2 + 1

    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        # 使用有界队列控制并发任务数量
        futures = []
        max_pending_tasks = max_workers * 2  # 最大待处理任务数

        tile_count = 0

        # 遍历每个层级（level）
        for level in range(dz_level_count):
            level_tile = dz.level_tiles[level]
            # 生成每个瓦片
            for y in range(level_tile[1]):
                for x in range(level_tile[0]):
                    # 控制并发任务数量
                    if len(futures) >= max_pending_tasks:
                        # 等待一些任务完成
                        completed_futures = []
                        for future in futures:
                            if future.done():
                                completed_futures.append(future)

                        for future in completed_futures:
                            futures.remove(future)
                            try:
                                future.result()
                                tile_count += 1
                                if tile_count % 100 == 0:
                                    print(f"Processed {tile_count} tiles")
                                    # 打印当前时间
                                    print(f"Current time: {time.strftime('%Y-%m-%d %H:%M:%S', time.localtime())}")
                            except Exception as e:
                                print(f"Error processing tile: {e}")
                    # 提交异步任务
                    future = executor.submit(dz.process_single_tile, level, x, y, output_dir)
                    futures.append(future)

        # 等待剩余任务完成
        for future in concurrent.futures.as_completed(futures):
            try:
                future.result()
                tile_count += 1
            except Exception as e:
                print(f"Error processing tile: {e}")

    end_time = time.time()
    elapsed_time = end_time - start_time
    print(f"Time taken to generate tiles: {elapsed_time:.2f} seconds")
    print(f"Total tiles processed: {tile_count}")

def main():
    # 获取传递给Python脚本的参数
    args = sys.argv[1:]  # sys.argv[0] 是脚本的名称，所以我们从索引 1 开始

    wsi_path = args[0]
    output_dir = args[1]
    print(f"wsi_path : {args[0]}")
    print(f"output_dir : {args[1]}")
    slide = openslide.OpenSlide(wsi_path)
    # Create an OpenSlideCache object with a specified capacity (e.g., 100 MB)
    # print(f"Slide dimensions: {slide.dimensions}")
    cache_capacity = 1 * 1024 * 1024 * 1024  # 1GB
    cache = OpenSlideCache(cache_capacity)
    slide.set_cache(cache)
    cunrrent_output_zoomify_tiles(slide, output_dir)
    slide.close()


if __name__ == "__main__":
    main()