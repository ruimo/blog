# ---

# --- !Ups

create index ix01_image_created_time on image (created_time);

# --- !Downs

drop index if exists ix01_image_created_time;
