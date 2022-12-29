CREATE SCHEMA vk_scan;
SET SEARCH_PATH TO 'vk_scan';
CREATE TABLE groups
(
    id int PRIMARY KEY,
    name text NULL,
    active boolean DEFAULT TRUE
);

CREATE TABLE groups_scan_info
(
    group_id int PRIMARY KEY
        CONSTRAINT groups_id_gsi REFERENCES groups,
    last_comment_id int,
    posts int[]
);

CREATE USER vk_scan_ro WITH PASSWORD 'password';
GRANT USAGE ON SCHEMA vk_scan to vk_scan_ro;
GRANT SELECT, INSERT ON ALL TABLES IN SCHEMA vk_scan to vk_scan_ro;
