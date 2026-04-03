# 基础镜像
FROM  openjdk:8-jre
# author
MAINTAINER staitech

# 安装 Python
RUN sed -i 's/archive.ubuntu.com/mirrors.aliyun.com/g' /etc/apt/sources.list && \
    sed -i 's/security.ubuntu.com/mirrors.aliyun.com/g' /etc/apt/sources.list && \
    apt-get update && \
    apt-get install -y python3 python3-pip && \
    ln -s /usr/bin/python3 /usr/bin/python && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* \
    && pip install openslide-bin

# 挂载目录
VOLUME /home/staitech/attachment
VOLUME /home/staitech/uploadPath
VOLUME /home/staitech/Slides
VOLUME /home/pacmvs
# 创建目录
RUN mkdir -p /home/staitech
RUN mkdir -p /home/pacmvs
RUN mkdir -p /home/staitech/openslide-java-jni
# 指定路径
WORKDIR /home/staitech

COPY ./script/requirements.txt  /tmp/requirements.txt
RUN pip install -r /tmp/requirements.txt -i https://mirrors.aliyun.com/pypi/simple

COPY ./opencv-java-jni  /usr/lib
COPY ./openslide-java-jni /usr/lib
COPY ./libvips/lib /usr/lib
COPY ./libvips/bin /usr/bin
RUN chmod -R 777 /usr/bin
CMD ldconfig

# 复制jar文件到路径
COPY ./jar/staitech-modules-image.jar /home/staitech/staitech-modules-image.jar
COPY ./script/deepzoom_custom.py /home/staitech/deepzoom_custom.py
COPY ./script/tile.py /home/staitech/tile.py
EXPOSE 9083
# 启动文件服务
ENTRYPOINT ["java","-Dfile.encoding=utf-8","-jar","staitech-modules-image.jar"]

