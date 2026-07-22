# Deployment Guide

Version: R14

---

# Local Development

Requirements

Java 21

Docker

Docker Compose

Maven

Git

---

# Start Infrastructure

docker compose up -d

Services

MySQL

Kafka

Redis

Kafka UI

---

# Build

mvn clean install

---

# Run

Config Server

↓

Discovery Server

↓

Gateway

↓

Business Services

---

# Ports

Gateway

8080

Config Server

8888

Discovery

8761

Kafka

9092

Redis

6379

MySQL

3306

Kafka UI

8081

---

# Deployment Order

1

Config Server

↓

2

Discovery

↓

3

Gateway

↓

4

Business Services

---

# Production

Future

Docker

↓

Kubernetes

↓

Ingress

↓

Gateway

↓

Microservices

↓

Kafka Cluster

↓

Redis Cluster

↓

MySQL

---

# Scaling

Gateway

Horizontal

Booking

Horizontal

Inventory

Horizontal

Payment

Horizontal

Notification

Horizontal

---

# Health Check

Spring Boot Actuator

/readiness

/liveness

/health

---

# Monitoring

Future

Micrometer

Prometheus

Grafana

OpenTelemetry

---

# Logging

Centralized Logging

Future

ELK

or

OpenSearch

---

# Backup

MySQL Backup

Redis Snapshot

Kafka Retention Policy

---

# Disaster Recovery

Multi AZ

Backup

Restore

Rolling Deployment

Zero Downtime Deployment
