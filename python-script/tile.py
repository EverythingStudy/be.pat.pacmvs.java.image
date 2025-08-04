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

def process_single_tile(dz, level, x, y, output_dir, slide):
    """
    处理单个瓦片
    """
    try:
        tile_path = os.path.join(output_dir, f"{level}-{x}-{y}.jpg")
        tile = dz.get_tile(level, (x, y))
        save_tile_to_file(tile, tile_path, slide)
        return f"Saved tile {tile_path}"
    except Exception as e:
        return f"Error processing tile {level}-{x}-{y}: {e}"

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

                    # tile_path = os.path.join(output_dir, f"{level}-{x}-{y}.jpg")
                    # 读取瓦片
                    # tile = dz.get_tile(level, (x, y))
                    # 提交异步任务
                    # future = executor.submit(save_tile_to_file, tile, tile_path, slide)
                    future = executor.submit(process_single_tile, dz, level, x, y, output_dir, slide)
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

def output_zoomify_tiles(slide, output_dir):

    """
    创建 Zoomify 格式的瓦片
    :param slide: OpenSlide 对象
    :param output_dir: 输出目录
    """
    start_time = time.time()
    # 创建输出目录
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
    dz = DeepZoomGeneratorCustom(slide,TILE_SIZE,0,True)
    dz_level_count = dz.level_count

    # 遍历每个层级（level）
    for level in range(dz_level_count):
        level_tile = dz.level_tiles[level]
        # 生成每个瓦片
        for y in range(level_tile[1]):
            for x in range(level_tile[0]):
                tile_path = os.path.join(output_dir, f"{level}-{x}-{y}.jpg")
                print(f"Saved tile {tile_path}")
                # 读取瓦片
                tile = dz.get_tile(level, (x, y))
                save_tile_to_file(tile, tile_path, slide)
    end_time = time.time()  # Record the end time
    elapsed_time = end_time - start_time  # Calculate the elapsed time
    print(f"Time taken to generate tiles: {elapsed_time:.2f} seconds")

def save_tile_to_file(tile, tile_path, slide):
    """
    保存瓦片图像到文件
    :param tile: 瓦片图像
    :param tile_path: 瓦片保存路径
    """
    if tile.mode == 'RGBA':
        tile = tile.convert('RGB')

    # 1. 生成纯色图像数据
    blank_image = np.zeros((TILE_SIZE, TILE_SIZE, 3), dtype=np.uint8)
    blank_image[:, :] = [255, 255, 255]  # 白色
    tile_np = np.array(tile)
    if (tile_np.shape[0]<TILE_SIZE) or (tile_np.shape[1]<TILE_SIZE):
        # blank_image[:, :tile_np.shape[0]] = [255, 255, 255]
        # tile_np合并到blank_image
        blank_image[:tile_np.shape[0], :tile_np.shape[1]] = tile_np
        # 将 NumPy 数组转换为 PIL 图像
        tile = Image.fromarray(blank_image)
    profile = slide.color_profile.profile
    rgbp = ImageCms.createProfile("sRGB")
    # 应用颜色转换
    transform = ImageCms.buildTransform(profile, rgbp, "RGB", "RGB")
    result = ImageCms.applyTransform(tile, transform)
    result.save(tile_path, "JPEG", quality=90)

def main():
    # 获取传递给Python脚本的参数
    args = sys.argv[1:]  # sys.argv[0] 是脚本的名称，所以我们从索引 1 开始
    print(f"Argument1 : {args[0]}")
    print(f"Argument2 : {args[1]}")
    wsi_path = args[0]
    # wsi_path = 'D:\work\WSI\R24-S030-RD 2312591-3 4F.svs'
    # wsi_path = "D:\work\python\\144-v1.0\V004-S001-RD 2269524-4 4M.svs"
    # wsi_path = "E:\R77-S686-RD 2269530-12 5M.svs"
    # wsi_path = "E:\R249-224-RD~2424912~2~4M~TN~RC-1_083944.svs"
    # print(wsi_path)
    slide = openslide.OpenSlide(wsi_path)
    # Create an OpenSlideCache object with a specified capacity (e.g., 100 MB)
    # print(f"Slide dimensions: {slide.dimensions}")
    cache_capacity = 1 * 1024 * 1024 * 1024  # 1GB
    cache = OpenSlideCache(cache_capacity)
    slide.set_cache(cache)
    # output_dir = 'path/to/output/zoomify_tiles'
    output_dir = args[1]
    # output_zoomify_tiles(slide, output_dir)
    cunrrent_output_zoomify_tiles(slide, output_dir)
    slide.close()


if __name__ == "__main__":
    main()