-- Schema for the in-memory R2DBC H2 database used by the dev profile.
-- Re-created on every start because application-dev.properties sets
-- spring.sql.init.mode=always.
CREATE TABLE IF NOT EXISTS Transaction (
    Id          VARCHAR(36)   PRIMARY KEY,
    OrderNumber VARCHAR(64)   NOT NULL,
    Item        VARCHAR(255)  NOT NULL,
    Qty         BIGINT        NOT NULL,
    Amount      NUMERIC(19,2) NOT NULL,
    Status      BOOLEAN       NOT NULL,
    CreatedDate TIMESTAMP     NOT NULL,
    UpdatedDate TIMESTAMP
);
