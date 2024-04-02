--liquibase formatted sql
--changeset joymutlu:1.0 splitStatements:false failOnError:true logicalFilePath:path-independent

CREATE TABLE anime
(
    id          BIGSERIAL,
    name        VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

--rollback DROP TABLE anime;






