ALTER TABLE users ADD COLUMN admin_provisioned BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE staff_cinema_assignments (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    cinema_id BINARY(16) NOT NULL,
    assigned_by BINARY(16) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_staff_cinema_user UNIQUE (user_id),
    CONSTRAINT fk_staff_cinema_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_staff_cinema_actor FOREIGN KEY (assigned_by) REFERENCES users (id),
    INDEX idx_staff_cinema_created (cinema_id, created_at, id),
    INDEX idx_staff_created (created_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
