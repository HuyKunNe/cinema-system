INSERT INTO roles (
    id,
    name,
    description,
    version,
    created_at,
    updated_at
)
VALUES
(
    UNHEX(REPLACE(
        '019c3000-0000-7000-8000-000000000001',
        '-',
        ''
    )),
    'USER',
    'Standard cinema platform user',
    0,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
),
(
    UNHEX(REPLACE(
        '019c3000-0000-7000-8000-000000000002',
        '-',
        ''
    )),
    'STAFF',
    'Cinema operational staff',
    0,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
),
(
    UNHEX(REPLACE(
        '019c3000-0000-7000-8000-000000000003',
        '-',
        ''
    )),
    'ADMIN',
    'Platform administrator',
    0,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
),
(
    UNHEX(REPLACE(
        '019c3000-0000-7000-8000-000000000004',
        '-',
        ''
    )),
    'SERVICE',
    'Internal service principal',
    0,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
);

INSERT INTO permissions (
    id,
    code,
    description,
    version,
    created_at,
    updated_at
)
VALUES
(
    UNHEX(REPLACE(
        '019c3000-0000-7100-8000-000000000001',
        '-',
        ''
    )),
    'booking:create',
    'Create a booking',
    0,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
),
(
    UNHEX(REPLACE(
        '019c3000-0000-7100-8000-000000000002',
        '-',
        ''
    )),
    'booking:read',
    'Read an authorized booking',
    0,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
),
(
    UNHEX(REPLACE(
        '019c3000-0000-7100-8000-000000000003',
        '-',
        ''
    )),
    'booking:cancel',
    'Cancel an authorized booking',
    0,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
),
(
    UNHEX(REPLACE(
        '019c3000-0000-7100-8000-000000000004',
        '-',
        ''
    )),
    'movie:manage',
    'Manage movies and genres',
    0,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
),
(
    UNHEX(REPLACE(
        '019c3000-0000-7100-8000-000000000005',
        '-',
        ''
    )),
    'showtime:manage',
    'Manage showtimes',
    0,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
),
(
    UNHEX(REPLACE(
        '019c3000-0000-7100-8000-000000000006',
        '-',
        ''
    )),
    'inventory:manage',
    'Manage cinema inventory',
    0,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
),
(
    UNHEX(REPLACE(
        '019c3000-0000-7100-8000-000000000007',
        '-',
        ''
    )),
    'payment:read',
    'Read payment status',
    0,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
),
(
    UNHEX(REPLACE(
        '019c3000-0000-7100-8000-000000000008',
        '-',
        ''
    )),
    'notification:manage',
    'Manage notification operations',
    0,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
),
(
    UNHEX(REPLACE(
        '019c3000-0000-7100-8000-000000000009',
        '-',
        ''
    )),
    'user:manage',
    'Manage users and authorization assignments',
    0,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
);

INSERT INTO role_permissions (
    role_id,
    permission_id,
    assigned_at,
    assigned_by_user_id
)
SELECT
    roles.id,
    permissions.id,
    CURRENT_TIMESTAMP(6),
    NULL
FROM roles
CROSS JOIN permissions
WHERE
    (
        roles.name = 'USER'
        AND permissions.code IN (
            'booking:create',
            'booking:read',
            'booking:cancel'
        )
    )
    OR
    (
        roles.name = 'STAFF'
        AND permissions.code IN (
            'booking:read',
            'booking:cancel',
            'movie:manage',
            'showtime:manage',
            'inventory:manage',
            'payment:read',
            'notification:manage'
        )
    )
    OR roles.name = 'ADMIN';
