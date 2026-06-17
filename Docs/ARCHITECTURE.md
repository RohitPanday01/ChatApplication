# Distributed Real-Time Chat System

## Master Architecture Document

Author: Rohit

Version: 1.0

Status: Active Development

Last Updated: YYYY-MM-DD

---

# 1. Vision

Build a production-oriented distributed real-time messaging platform demonstrating:

* Distributed systems design
* Event-driven architecture
* WebSocket communication
* Kafka messaging
* Redis state management
* PostgreSQL data modeling
* Cloud deployment
* Observability
* Load testing
* Fault tolerance

Target scale:

* 3,000–5,000 active concurrent websocket users
* 5M+ persisted messages
* Multi-node deployment
* Sub-100ms end-to-end message delivery

---

# 2. Non-Goals

Not attempting to build:

* End-to-end encryption
* Voice/video calling
* Media CDN
* Multi-region active-active replication
* Billion-user scale

Current goal:

Production-quality engineering and scalability patterns.

---

# 3. Technology Stack

Backend:

* Java 21
* Spring Boot 3

Real Time:

* Raw Spring WebSocketHandler
* No STOMP

Messaging:

* Apache Kafka (KRaft)

Storage:

* PostgreSQL

State:

* Redis

Infrastructure:

* Docker
* Docker Compose
* AWS EC2

Monitoring:

* Prometheus
* Grafana

Testing:

* Locust

---

# 4. High-Level Architecture

Client
↓
Load Balancer
↓
Spring Boot Nodes
↓
Kafka
↓
Redis
↓
PostgreSQL

Application nodes are stateless.

Redis stores ephemeral state.

Kafka handles event propagation.

PostgreSQL stores durable state.

---

# 5. Core Domain Model

User

PrivateChannel

GroupChannel

PrivateMessage

GroupMessage

ChatParticipantState

GroupMember

GroupInvitation

---

# 6. WebSocket Architecture

## Why Raw WebSockets?

STOMP introduces:

* Frame overhead
* Additional allocations
* Broker semantics not needed

Raw WebSocketHandler provides:

* Lower latency
* Fewer allocations
* Greater control

Chosen handler:

PresenceWSHandler

---

# 7. Authentication Architecture

JWT-based authentication.

Handshake flow:

Client
→ JWT Cookie
→ JwtCookieHandshakeInterceptor
→ Signature Validation
→ Extract username + userId
→ Session Attributes

No database lookup occurs during handshake.

Reason:

Database lookups during reconnect storms are catastrophic.

---

# 8. Session Lifecycle

Connect:

Client Connect
→ PresenceWSHandler
→ RegisterUserSession
→ Redis Presence Registration
→ Channel Subscription

Disconnect:

Socket Closed
→ Session Cleanup
→ Redis Cleanup
→ Presence Event

Current limitation:

Single session per user.

Future:

Multiple devices per user.

---

# 9. Presence Architecture

Source of truth:

Redis

Key:

presence:user:{username}

Value:

nodeId

Example:

presence:user:rohit = node-2

Purpose:

Route messages to the node currently hosting the websocket session.

---

# 10. Message Lifecycle

## Step 1

Client sends message.

PrivateChannelController receives.

## Step 2

DirectMessageProducer publishes to Kafka.

Topics:

dm-persist
dm-delivery

## Step 3

Persistence consumer stores message.

## Step 4

Delivery consumer routes message.

## Step 5

Read receipt emitted.

---

# 11. Kafka Topic Architecture

dm-persist

Purpose:
Persist messages.

Consumer:
DMPersistenceListener

---

dm-delivery

Purpose:
Deliver messages.

Consumer:
DMDeliveryListener

---

read-receipt

Purpose:
Read and delivered receipts.

Consumer:
ReadReceiptConsumer

---

notification

Purpose:
Offline notifications.

Consumer:
NotificationConsumer

---

inter-node-delivery

Purpose:
Cross-node websocket routing.

Consumer:
InterNodeListener

---

# 12. Message Ordering Strategy

Current strategy:

Snowflake IDs

Structure:

41 bits timestamp
10 bits worker id
12 bits sequence

Advantages:

* No DB coordination
* No Redis coordination
* Globally unique
* Sortable

Message ordering is derived from message_id.

---

# 13. Read Receipt Architecture

Receipt Types:

DELIVERED
READ

Flow:

Message Delivered
→ ReadReceiptProducer
→ Kafka
→ ReadReceiptConsumer
→ Sender WebSocket

---

# 14. ChatParticipantState

Stores:

channel_id
user_id
last_delivered_seq
last_read_seq

Purpose:

Unread counts.

Read tracking.

Delivery tracking.

No message table scans required.

---

# 15. Typing Architecture

Uses Redis PubSub.

Producer:
TypingEventPublisher

Consumer:
TypingSubscriber

Current Risk:

Listener-per-channel design may not scale.

Future improvement:

Single subscriber with internal routing.

---

# 16. Redis Key Design

Presence:

presence:user:{username}

Typing:

typing:{channelId}

Last Seen:

user:lastseen:{userId}

Future:

Document every Redis key here.

---

# 17. Failure Scenarios

## Kafka Consumer Crash

Consumer restarts.

Kafka replay occurs.

System relies on idempotent event handling.

---

## Node Crash

Redis may contain stale ownership.

Requires TTL/heartbeat strategy.

---

## Redis Restart

Presence rebuilt through reconnects.

Transient routing failures possible.

---

## PostgreSQL Outage

Persistence consumers fail.

Kafka retains events.

Processing resumes after recovery.

---

# 18. Load Testing Plan

Tool:

Locust

Phase 1:

50 users

Phase 2:

200 users

Phase 3:

500 users

Phase 4:

1000 users

Phase 5:

3000-5000 users

Metrics:

CPU
Memory
GC
Kafka Lag
Redis Ops
WebSocket Count

---

# 19. JVM Tuning

Java 21

G1GC

-Xms4500m
-Xmx4500m

Target:

Low pause times.

Container-aware execution.

---

# 20. Deployment Architecture

AWS

Application:
2 EC2

Kafka:
3 Nodes

Redis:
1 Node

PostgreSQL:
1 Node

Prometheus:
1 Node

Grafana:
1 Node

---

# 21. Known Technical Debt

Typing subscription model.

Single-session limitation.

Presence lease correctness.

Reconnect storm validation pending.

---

# 22. Architecture Decisions (ADR)

ADR-001:
Use Raw WebSockets instead of STOMP.

ADR-002:
Use Kafka for event propagation.

ADR-003:
Use Redis for ephemeral state.

ADR-004:
Use Snowflake IDs for ordering.

ADR-005:
Use ChatParticipantState for unread calculations.

ADR-006:
Use keyset pagination.

---

# 23. Future Roadmap

Multi-device support

Reconnect storm testing

Presence lease redesign

CI/CD

Terraform

AWS deployment

Production load testing

Performance benchmarking
