UPDATE oauth2_registered_client
SET scopes =
    'openid,profile,email,booking:create,booking:read,booking:cancel,movie:manage,showtime:manage,inventory:manage,payment:read,user:manage'
WHERE client_id = 'cinema-web';

UPDATE oauth2_registered_client
SET client_settings = JSON_SET(
    client_settings,
    '$."settings.client.require-authorization-consent"',
    FALSE
)
WHERE client_id = 'cinema-web';
