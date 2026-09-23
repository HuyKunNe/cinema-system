# config run at local

## config-service

.\mvnw.cmd -pl infrastructure/config-service spring-boot:run

## discovery-service

```text
$env:CONFIG_SERVER_URL = "http://localhost:8888"
.\mvnw.cmd -pl infrastructure/discovery-service spring-boot:run
```

## User Service

```text
$privateKey = (Resolve-Path .\.local\keys\user-service-private.pem).Path.Replace('\','/')
$publicKey = (Resolve-Path .\.local\keys\user-service-public.pem).Path.Replace('\','/')

$env:CONFIG_SERVER_URL = "http://localhost:8888"
$env:EUREKA_URL = "http://localhost:8761/eureka/"
$env:USER_DB_URL = "jdbc:mysql://localhost:3306/cinema_user_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:USER_DB_USERNAME = "<local_username>"
$env:USER_DB_PASSWORD = "<local_password>"
$env:USER_AUTHORIZATION_SERVER_ISSUER = "http://localhost:8082"
$env:USER_JWT_AUDIENCES = "cinema-api"
$env:USER_JWT_SIGNING_ENABLED = "true"
$env:USER_JWT_SIGNING_KEY_ID = "cinema-local-key-1"
$env:USER_JWT_PRIVATE_KEY_LOCATION = "file:$privateKey"
$env:USER_JWT_PUBLIC_KEY_LOCATION = "file:$publicKey"
$env:CINEMA_LOCAL_BOOTSTRAP_ENABLED = "true"
.\mvnw.cmd -pl services/user-service spring-boot:run
```

## movie-service

$env:CONFIG_SERVER_URL = "http://localhost:8888"
$env:EUREKA_SERVER_URL = "http://localhost:8761/eureka/"
$env:MOVIE_DB_URL = "jdbc:mysql://localhost:3306/cinema_movie_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:MOVIE_DB_USERNAME = "<local_username>"
$env:MOVIE_DB_PASSWORD = "<local_password>"
$env:CINEMA_AUTH_ISSUER = "http://localhost:8082"
$env:CINEMA_AUTH_JWK_SET_URI = "http://localhost:8082/oauth2/jwks"
$env:CINEMA_AUTH_AUDIENCE = "cinema-api"
.\mvnw.cmd -pl services/movie-service spring-boot:run

## Inventory Service

$env:CONFIG_SERVER_URL = "http://localhost:8888"
$env:EUREKA_URL = "http://localhost:8761/eureka/"
$env:INVENTORY_DB_URL = "jdbc:mysql://localhost:3306/cinema_inventory_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:INVENTORY_DB_USERNAME = "<local_username>"
$env:INVENTORY_DB_PASSWORD = "<local_password>"
$env:CINEMA_AUTH_ISSUER = "http://localhost:8082"
$env:CINEMA_AUTH_JWK_SET_URI = "http://localhost:8082/oauth2/jwks"
$env:CINEMA_AUTH_AUDIENCE = "cinema-api"
$env:KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
$env:INVENTORY_KAFKA_ENABLED = "true"
.\mvnw.cmd -pl services/inventory-service spring-boot:run

## Booking Service

$env:CONFIG_SERVER_URL = "http://localhost:8888"
$env:EUREKA_SERVER_URL = "http://localhost:8761/eureka/"
$env:BOOKING_DB_URL = "jdbc:mysql://localhost:3306/cinema_booking_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:BOOKING_DB_USERNAME = "<local_username>"
$env:BOOKING_DB_PASSWORD = "<local_password>"
$env:CINEMA_AUTH_ISSUER = "http://localhost:8082"
$env:CINEMA_AUTH_JWK_SET_URI = "http://localhost:8082/oauth2/jwks"
$env:CINEMA_AUTH_AUDIENCE = "cinema-api"
$env:KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
$env:BOOKING_KAFKA_ENABLED = "true"
.\mvnw.cmd -pl services/booking-service spring-boot:run

## Payment Service

$env:CONFIG_SERVER_URL = "http://localhost:8888"
$env:EUREKA_SERVER_URL = "http://localhost:8761/eureka/"
$env:PAYMENT_DB_URL = "jdbc:mysql://localhost:3306/cinema_payment_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:PAYMENT_DB_USERNAME = "<local_username>"
$env:PAYMENT_DB_PASSWORD = "<local_password>"
$env:CINEMA_AUTH_ISSUER = "http://localhost:8082"
$env:CINEMA_AUTH_JWK_SET_URI = "http://localhost:8082/oauth2/jwks"
$env:CINEMA_AUTH_AUDIENCE = "cinema-api"
$env:KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
$env:PAYMENT_KAFKA_ENABLED = "true"
$env:PAYMENT_PROVIDER = "MOCK"
$env:PAYMENT_PROVIDER_OPERATION_SCHEDULING_ENABLED = "true"
.\mvnw.cmd -pl services/payment-service spring-boot:run

## Gateway Service

```text
$env:CONFIG_SERVER_URL = "http://localhost:8888"
$env:EUREKA_SERVER_URL = "http://localhost:8761/eureka/"
$env:CINEMA_AUTH_ISSUER = "http://localhost:8082"
$env:CINEMA_AUTH_JWK_SET_URI = "http://localhost:8082/oauth2/jwks"
$env:CINEMA_AUTH_AUDIENCE = "cinema-api"
.\mvnw.cmd -pl infrastructure/gateway-service spring-boot:run
```

### create RAS key

mkdir -p .local/keys
New-Item -ItemType Directory -Force .\local-keys
openssl genpkey -algorithm RSA `  -pkeyopt rsa_keygen_bits:2048`
-out .\local-keys\jwt-private.pem

openssl rsa `  -pubout`
-in .\local-keys\jwt-private.pem `
-out .\local-keys\jwt-public.pem

| Service   |   Port | Swagger                                 |
| --------- | -----: | --------------------------------------- |
| Movie     | `8081` | `http://localhost:8081/swagger-ui.html` |
| User      | `8082` | `http://localhost:8082/swagger-ui.html` |
| Inventory | `8083` | `http://localhost:8083/swagger-ui.html` |
| Booking   | `8084` | `http://localhost:8084/swagger-ui.html` |
| Payment   | `8085` | `http://localhost:8085/swagger-ui.html` |
