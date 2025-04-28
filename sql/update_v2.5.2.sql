alter table tb_image
    add process_flag  varchar(255) not null default '5' comment '处理状态，1-解析中、2-解析失败、3-可用、4-上传失败、5-上传中';
alter table tb_image
    modify wax_code varchar(255) null comment '蜡块号';

alter table tb_image
    modify create_time datetime default current_timestamp not null comment '创建时间';

alter table tb_image
    modify update_time datetime default current_timestamp not null comment '更新时间';