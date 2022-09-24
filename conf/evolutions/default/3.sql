# ---

# --- !Ups

create sequence article_tag_seq start with 1;

create table article_tag (
  article_tag_id bigint not null,
  article_id bigint not null,
  tag_name varchar(64) not null,
  constraint pk_article_tag primary key (article_tag_id)
);

create unique index article_tag_tag_name on article_tag (article_id, tag_name);

create index article_tag_article_id on article_tag (article_id);

alter table article_tag
    add constraint article_tag_article_id_fkey foreign key (article_id) references article(article_id) on delete cascade;

# --- !Downs

drop table article_tag;

drop sequence article_tag_seq;
