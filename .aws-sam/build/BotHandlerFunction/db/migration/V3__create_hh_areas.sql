CREATE TABLE hh_areas (
    id          VARCHAR(20) PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    parent_id   VARCHAR(20),
    name_lower  VARCHAR(255) NOT NULL
);

CREATE INDEX idx_hh_areas_name_lower ON hh_areas(name_lower);
