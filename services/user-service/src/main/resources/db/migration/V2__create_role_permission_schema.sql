CREATE TABLE roles (
    id BINARY(16) NOT NULL,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_roles
        PRIMARY KEY (id),

    CONSTRAINT uk_roles_name
        UNIQUE (name),

    CONSTRAINT chk_roles_name
        CHECK (
            name IN (
                'USER',
                'STAFF',
                'ADMIN',
                'SERVICE'
            )
        )
);

CREATE TABLE permissions (
    id BINARY(16) NOT NULL,
    code VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_permissions
        PRIMARY KEY (id),

    CONSTRAINT uk_permissions_code
        UNIQUE (code)
);

CREATE TABLE user_roles (
    user_id BINARY(16) NOT NULL,
    role_id BINARY(16) NOT NULL,
    assigned_at DATETIME(6) NOT NULL,
    assigned_by_user_id BINARY(16) NULL,

    CONSTRAINT pk_user_roles
        PRIMARY KEY (user_id, role_id),

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id)
        REFERENCES users (id),

    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id)
        REFERENCES roles (id),

    CONSTRAINT fk_user_roles_assigned_by
        FOREIGN KEY (assigned_by_user_id)
        REFERENCES users (id)
);

CREATE INDEX idx_user_roles_role
    ON user_roles (role_id);

CREATE INDEX idx_user_roles_assigned_by
    ON user_roles (assigned_by_user_id);

CREATE TABLE role_permissions (
    role_id BINARY(16) NOT NULL,
    permission_id BINARY(16) NOT NULL,
    assigned_at DATETIME(6) NOT NULL,
    assigned_by_user_id BINARY(16) NULL,

    CONSTRAINT pk_role_permissions
        PRIMARY KEY (role_id, permission_id),

    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id)
        REFERENCES roles (id),

    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id)
        REFERENCES permissions (id),

    CONSTRAINT fk_role_permissions_assigned_by
        FOREIGN KEY (assigned_by_user_id)
        REFERENCES users (id)
);

CREATE INDEX idx_role_permissions_permission
    ON role_permissions (permission_id);

CREATE INDEX idx_role_permissions_assigned_by
    ON role_permissions (assigned_by_user_id);
