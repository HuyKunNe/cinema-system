# Architecture

## High Level Architecture

                    +----------------------+
                    |      Client          |
                    +----------+-----------+
                               |
                               |
                               v
                    +----------------------+
                    |  Gateway Service     |
                    +----------+-----------+
                               |
         +---------------------+----------------------+
         |                     |                      |
         v                     v                      v

 Booking Service       Movie Service        User Service

         |

         |

         +-----------------------------+

                                       |

                                       v

                              Kafka Event Bus

                                       |

                 +---------------------+----------------+

                 |                                      |

                 v                                      v

         Payment Service                    Inventory Service

                 |

                 v

       Notification Service

---

# Infrastructure

Gateway

↓

Discovery

↓

Config Server

↓

Microservices

↓

Kafka

↓

Redis

↓

MySQL

---

# Booking Flow

Client

↓

Gateway

↓

Booking Service

↓

Distributed Lock

↓

Transaction

↓

Bookings

↓

Booking Seats

↓

Show Seats

↓

Outbox Event

↓

Commit

↓

Scheduler

↓

Kafka

↓

Payment Service

↓

Payment Success

↓

Kafka

↓

Booking Confirmed

---

# Event Driven

The system is asynchronous.

Services communicate using Kafka.

No service calls another service directly for business workflow.

---

# Saga

Pattern:

Saga Choreography

No Orchestrator.

Each service reacts to events.

---

# Transactional Outbox

Business Transaction

+

Outbox Event

commit together.

Scheduler publishes later.

---

# Distributed Lock

Redis + Redisson

Lock Key

showtimeId

+

seat numbers

Prevent double booking.

---

# Idempotent Consumer

Each consumer stores processed event ids.

Duplicate messages are ignored.

---

# Common Modules

Shared libraries are isolated.

Business logic never belongs inside common modules.

---

# Service Independence

Each service owns:

Database

Domain

Repository

Entities

Events

No shared database.

---

# Scalability

Stateless Services

Horizontal Scaling

Kafka Partition

Redis Cluster

MySQL Read Replica

Docker Ready

Cloud Ready
