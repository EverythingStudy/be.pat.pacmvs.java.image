alter table tb_image
    add process_flag  varchar(255) not null default '5' comment '处理状态，1-解析中、2-解析失败、3-可用、4-上传失败、5-上传中';
alter table tb_image
    alter column create_time set default (CURRENT_TIMESTAMP);
alter table tb_image
    alter column update_time set default (CURRENT_TIMESTAMP);