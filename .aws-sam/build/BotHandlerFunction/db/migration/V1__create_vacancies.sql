CREATE TABLE vacancies (
    id           BIGSERIAL PRIMARY KEY,
    title        VARCHAR(500) NOT NULL,
    company      VARCHAR(255) NOT NULL,
    url          VARCHAR(1000) NOT NULL UNIQUE,
    description  TEXT,
    location     VARCHAR(255),
    salary       VARCHAR(255),
    site_key     VARCHAR(50),
    found_at     TIMESTAMP NOT NULL,
    updated_at   TIMESTAMP NOT NULL,
    content_hash VARCHAR(64) NOT NULL
);

CREATE INDEX idx_vacancies_url ON vacancies(url);
CREATE INDEX idx_vacancies_site_key ON vacancies(site_key);
