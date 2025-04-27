alter table tb_image
    add process_flag  varchar(255) not null default '5' comment '处理状态，1-解析中、2-解析失败、3-可用、4-上传失败、5-上传中';
alter table tb_image
    modify wax_code varchar(255) null comment '蜡块号';