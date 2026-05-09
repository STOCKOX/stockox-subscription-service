
# 💳 StockOX Subscription Service

A production-ready **Spring Boot microservice** that handles subscription management, Razorpay payment integration, plan enforcement, and PDF invoice generation for the StockOX platform.

---

## 🧩 Overview

The `stockox-subscription-service` is a standalone billing and subscription microservice within the StockOX ecosystem. It manages tenant subscription lifecycles — from free STARTER plans to paid upgrades — and integrates with Razorpay for secure payment processing. Invoices are auto-generated as PDFs and emailed to tenants on successful payment.

---

## ✨ Features

- 📦 **Subscription Management** — Create, upgrade, cancel, and auto-expire tenant subscriptions
- 💰 **Razorpay Integration** — Create payment orders and verify signatures securely
- 🧾 **PDF Invoice Generation** — Auto-generate and email invoices using iText 7 on successful payment
- 🔒 **JWT Authentication** — Stateless security via JWT tokens with a custom filter chain
- 🚦 **Plan Limit Enforcement** — Per-plan feature limits enforced via Redis caching
- ⏰ **Subscription Expiry Scheduler** — Background job to auto-expire overdue subscriptions
- 🔗 **Feign Client Integration** — Communicates with `product-service` for product count checks
- 📊 **Payment History** — Paginated transaction history with invoice re-send support
- 🌐 **CORS Support** — Configurable cross-origin settings for frontend integration

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.2.4 |
| Language | Java 17 |
| Database | PostgreSQL |
| Cache | Redis |
| Payment Gateway | Razorpay |
| PDF Generation | iText 7 (kernel + layout) |
| Email | Spring Mail (Zoho SMTP) |
| Security | Spring Security + JWT (jjwt 0.12.3) |
| Service Discovery | Netflix Eureka Client |
| Inter-Service Calls | OpenFeign |
| Mapping | MapStruct 1.5.5 |
| Boilerplate Reduction | Lombok |
| Build Tool | Maven |

---

## 📁 Project Structure

```
src/main/java/stockox_subscription_service/
├── config/           # Security, Redis, CORS, Razorpay, Feign, Async configs
├── controller/       # REST controllers (Plan, Subscription, Payment, Limit)
├── data/             # Plan data seeder on startup
├── dto/
│   ├── request/      # Request DTOs (Subscribe, Upgrade, Cancel, Payment)
│   └── response/     # Response DTOs (Plan, Subscription, Payment, Limits)
├── entity/           # JPA entities (Plan, Subscription, PaymentTransaction)
├── enums/            # PlanTier, SubscriptionStatus, PaymentStatus
├── exception/        # Custom exceptions + GlobalExceptionHandler
├── fiegn/            # Feign client for product-service
├── helper/           # SubscriptionContextHelper (JWT context extraction)
├── mapper/           # MapStruct mappers
├── repository/       # Spring Data JPA repositories
├── scheduler/        # Subscription expiry scheduler
├── security/         # JwtUtil + JwtAuthFilter
└── service/          # Service interfaces + implementations
```

---

## 🔌 API Endpoints

### Subscription — `/api/v1/subscriptions`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/current` | Get current tenant subscription |
| `PUT` | `/upgrade` | Upgrade or change plan |
| `DELETE` | `/cancel` | Cancel active subscription |
| `POST` | `/init/{tenantId}` | Initialize STARTER subscription for new tenant (internal) |

### Payment — `/api/v1/payments`

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/create-order` | Create a Razorpay payment order |
| `POST` | `/verify` | Verify payment signature and activate plan |
| `GET` | `/history` | Get paginated payment history |
| `GET` | `/{transactionId}/invoice` | Download PDF invoice |
| `POST` | `/{transactionId}/resend-invoice` | Re-send invoice to registered email |

### Plans — `/api/v1/plans`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/` | Get all available plans |

### Limits — `/api/v1/limits`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/check` | Check feature limits for current tenant |
| `GET` | `/all` | Get all limit details for current tenant |

---

## ⚙️ Configuration

Key settings in `application.yml`:

```yaml
server:
  port: 8082

spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/stockox_subscriptions_dev

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}

razorpay:
  key:
    id: ${RAZORPAY_KEY_ID}
    secret: ${RAZORPAY_KEY_SECRET}
  currency: INR

subscription:
  trial-days: 14
  gst-rate: 0.18

app:
  jwt:
    secret: ${JWT_SECRET}
```

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- PostgreSQL running on port `5433`
- Redis running on port `6379`
- Maven 3.8+
- Razorpay account (test or live keys)

### Run Locally

```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/stockox-subscription-service.git
cd stockox-subscription-service

# Set environment variables
export JWT_SECRET=your_jwt_secret_min_32_chars
export RAZORPAY_KEY_ID=rzp_test_xxxx
export RAZORPAY_KEY_SECRET=your_razorpay_secret

# Build and run
./mvnw spring-boot:run
```

The service starts on **`http://localhost:8082`**

---

## 🔐 Environment Variables

| Variable | Description | Default |
|---|---|---|
| `JWT_SECRET` | JWT signing secret (min 32 chars) | Dev key (change in prod!) |
| `RAZORPAY_KEY_ID` | Razorpay API Key ID | Test key |
| `RAZORPAY_KEY_SECRET` | Razorpay API Secret | Test secret |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `PRODUCT_SERVICE_URL` | Product microservice URL | `http://localhost:8083` |
| `USER_SERVICE_URL` | User microservice URL | `http://localhost:8081` |
| `FRONTEND_URL` | Frontend origin for CORS | `http://localhost:3000` |

---

## 🧪 Running Tests

```bash
./mvnw test
```

---

## 📦 Build JAR

```bash
./mvnw clean package -DskipTests
java -jar target/stockox-subscription-service-0.0.1-SNAPSHOT.jar
```

---

## 📝 Notes

- This service is part of the larger **StockOX microservices platform**
- Eureka and Spring Cloud Config are disabled by default for local development
- The `target/` folder is excluded from version control via `.gitignore`
- Plan data is seeded automatically on startup via `PlanDataSeeder`

---

## 📄 License

This project is proprietary to **StockOX India**. All rights reserved.
