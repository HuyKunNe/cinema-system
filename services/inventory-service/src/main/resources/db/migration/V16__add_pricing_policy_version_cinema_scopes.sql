ALTER TABLE cinema_pricing_policies
    DROP FOREIGN KEY fk_cinema_pricing_policies_cinema,
    DROP INDEX uk_cinema_pricing_policies_request;

ALTER TABLE cinema_pricing_policies
    CHANGE COLUMN cinema_id creation_cinema_id BINARY(16) NOT NULL,
    ADD COLUMN creation_scope_fingerprint VARCHAR(64)
        CHARACTER SET ascii COLLATE ascii_bin NULL;

UPDATE cinema_pricing_policies
SET creation_scope_fingerprint =
    LOWER(
        SHA2(
            CONCAT(
                '32:',
                LOWER(HEX(creation_cinema_id))
            ),
            256
        )
    );

ALTER TABLE cinema_pricing_policies
    MODIFY COLUMN creation_scope_fingerprint VARCHAR(64)
        CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    ADD CONSTRAINT fk_pricing_policy_creation_cinema
        FOREIGN KEY (creation_cinema_id)
        REFERENCES cinemas (id),
    ADD CONSTRAINT uk_pricing_policy_creation_request
        UNIQUE (creation_cinema_id, client_request_id);

ALTER TABLE cinema_pricing_policy_versions
    ADD COLUMN cinema_scope_revision BIGINT NOT NULL DEFAULT 0;

CREATE TABLE pricing_policy_version_cinemas (
    policy_version_id BINARY(16) NOT NULL,
    cinema_id BINARY(16) NOT NULL,

    PRIMARY KEY (policy_version_id, cinema_id),

    KEY idx_pricing_version_cinemas_cinema (
        cinema_id,
        policy_version_id
    ),

    CONSTRAINT fk_pricing_version_cinemas_version
        FOREIGN KEY (policy_version_id)
        REFERENCES cinema_pricing_policy_versions (id),

    CONSTRAINT fk_pricing_version_cinemas_cinema
        FOREIGN KEY (cinema_id)
        REFERENCES cinemas (id)
);

INSERT INTO pricing_policy_version_cinemas (
    policy_version_id,
    cinema_id
)
SELECT
    revision.id,
    policy.creation_cinema_id
FROM cinema_pricing_policy_versions revision
JOIN cinema_pricing_policies policy
    ON policy.id = revision.policy_id;
