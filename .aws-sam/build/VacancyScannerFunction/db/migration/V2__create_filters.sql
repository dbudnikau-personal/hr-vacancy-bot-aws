CREATE TABLE vacancy_filters (
    id          BIGSERIAL PRIMARY KEY,
    chat_id     BIGINT NOT NULL,
    name        VARCHAR(255) NOT NULL,
    keywords    VARCHAR(500),
    location    VARCHAR(255),
    salary_min  VARCHAR(50),
    active      BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE filter_sites (
    filter_id   BIGINT REFERENCES vacancy_filters(id) ON DELETE CASCADE,
    site_key    VARCHAR(50) NOT NULL
);

CREATE INDEX idx_filters_chat_id ON vacancy_filters(chat_id);
