# --- First database schema

# --- !Ups

create sequence blogger_seq start with 1;

create table blogger (
  blogger_id bigint not null,
  blogger_name varchar(64) not null,
  first_name varchar(64) not null,
  middle_name varchar(64),
  last_name varchar(64) not null,
  email varchar(255) not null,
  password_hash bigint not null,
  salt bigint not null,
  deleted boolean not null,
  constraint pk_blogger primary key (blogger_id)
);

create unique index blogger_blogger_name on blogger (blogger_name);

create sequence article_seq start with 1;

create table article (
  article_id bigint not null,
  title varchar(256) not null,
  body text not null,
  blogger_id bigint not null references blogger,
  publish_time timestamp not null,
  created_time timestamp not null default current_timestamp,
  updated_time timestamp not null default current_timestamp,
  constraint pk_article primary key (article_id)
);

create sequence comment_seq start with 1;

create table comment (
  comment_id bigint not null,
  article_id bigint not null references article on delete cascade,
  name varchar(64),
  body text not null,
  authorized boolean not null default false,
  created_time timestamp not null default current_timestamp,
  constraint pk_comment primary key (comment_id)
);

create sequence image_seq start with 1;

create table image (
  image_id bigint not null,
  file_name text not null,
  content_type text,
  thumbnail blob,
  body blob not null,
  created_time timestamp not null default current_timestamp,
  constraint pk_image primary key (image_id)
);

# --- !Downs

drop table comment;
drop sequence comment_seq;

drop table article;
drop sequence article_seq;

drop index if exists blogger_blogger_name;
drop table blogger;
drop sequence blogger_seq;

drop table image;
drop sequence image_seq;
