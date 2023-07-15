use `test`;

create table user
(
    id        bigint primary key comment '用户id',
    user_name varchar(120) not null default '' comment '用户名称',
    age       smallint     not null default -1 comment '年龄'
) comment '用户' charset = 'utf8mb4';

insert into user(id, user_name, age)
values (1, '张三', 23),
       (2, '李四', 25),
       (3, '王麻子', 26);