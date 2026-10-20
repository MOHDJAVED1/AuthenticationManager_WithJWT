# 🛡️ Secure Auth API

> A **Spring Boot REST API** demonstrating secure authentication, JWT-based authorization, role management, account protection, and API rate limiting.

---

## 📑 Table of Contents

| # | Section |
|---|---------|
| 1 | [Overview](#-overview) |
| 2 | [Technology Stack](#-technology-stack) |
| 3 | [Features](#-features) |
| 4 | [User Roles and Permissions](#-user-roles-and-permissions) |
| 5 | [API Endpoints](#-api-endpoints) |
| 6 | [Authentication Flow](#-authentication-flow) |
| 7 | [Security Implementation](#-security-implementation) |
| 8 | [Configuration](#-configuration) |
| 9 | [Database](#-database) |
| 10 | [Run the Application](#️-run-the-application) |
| 11 | [Testing with Postman](#-testing-with-postman) |
| 12 | [Production Deployment Checklist](#-production-deployment-checklist) |
| 13 | [Known Limitations and Future Improvements](#️-known-limitations-and-future-improvements) |
| 14 | [Security Disclaimer](#️-security-disclaimer) |

---

## 🎯 Overview

**Secure Auth API** is a backend authentication and authorization project built with **Spring Boot** and **Spring Security**.

It provides REST APIs for:

- ✅ User registration
- ✅ Login / JWT authentication
- ✅ Logout
- ✅ Password recovery
- ✅ Administrative operations
- ✅ Protection against excessive API requests

> 💡 The project is intended as a practical implementation of common backend security patterns and can be extended for enterprise applications.

---

## 🧰 Technology Stack

| Technology | Purpose |
|------------|---------|
| ☕ **Java 17-compatible** | Language runtime |
| 🌱 **Spring Boot 3.x** | Application framework |
| 🔐 **Spring Security** | Authentication & authorization |
| 🎫 **Spring Security OAuth2 Resource Server** | JWT validation |
| 🔑 **JWT (JSON Web Token)** | Token-based auth |
| 📦 **Maven** | Build tool |
| 🗄️ **Spring Data JPA / Hibernate** | ORM |
| 🐬 **MySQL 8** | Database |
| 🧩 **Lombok** | Boilerplate reduction |
| 🚦 **Bucket4j** | Rate limiting |
| 🧪 **Postman** | API testing |
| 📧 **Spring Mail** | Email functionality |

> ⚠️ **Note:** Verify the exact dependency versions in `pom.xml` before publishing the project.

---

## ✨ Features

### 1️⃣ User Registration

- REST API for user registration
- Password hashing using Spring Security's `PasswordEncoder`
- Persistence of user information in MySQL
- Validation and duplicate-email checks

### 2️⃣ Login and JWT Authentication

- Login using registered credentials
- Password verification using `PasswordEncoder.matches()`
- JWT generation after successful authentication
- JWT claims for user identity and role
- Bearer-token authentication for protected APIs
- **Stateless** Spring Security configuration

### 3️⃣ Role-Based Access Control

The application supports the following roles:

- 🟢 `ROLE_USER`
- 🟠 `ROLE_ADMIN`
- 🔴 `ROLE_SUPER_ADMIN`

Also uses user-type values for super-admin variants:

- `PRIMARY_SUPER_ADMIN`
- `BACKUP_SUPER_ADMIN`

> 🛡️ Protected administrative operations **must** be restricted to the appropriate roles.

### 4️⃣ Logout and Token Revocation

- Logout API for invalidating a JWT before its natural expiration
- Revoked-token tracking using a MySQL table
- Token identifier (`jti`) used for revocation tracking
- Authentication **rejects** revoked tokens
- Revoked tokens no longer authorize access to protected resources

### 5️⃣ Forgot Password and Reset Password

- Forgot-password API
- Reset-password API
- Password updates using the configured password encoder
- Support for password-reset tokens

> 🔒 **Security Requirement:** Password-reset tokens must be **cryptographically unpredictable**, **short-lived**, **single-use**, and **securely stored**. Reset tokens must **never** be exposed in production API responses or application logs.

### 6️⃣ Admin Registration and Invitations

- Super-admin-controlled admin onboarding
- Admin registration using an **invitation flow**
- Invitation endpoint: `POST /api/v1/auth/admin-registration`
- Expiring and single-use invitation tokens
- Server-side enforcement of:
  - Invitation validation
  - Token expiration
  - Token reuse prevention
  - Role assignment

### 7️⃣ Account Lockout

Login flow includes configurable protection against repeated incorrect passwords.

**Example configuration:**

```properties
security.login.max-failed-attempts=5
security.login.lock-duration-minutes=240
```

> 🔒 With these settings, an account is locked after **5 failed attempts** for **4 hours**.

The implementation tracks failed attempts and lock timestamps. Successful login resets the failure counter and clears expired locks according to the intended policy.

> ⚠️ **Warning:** The lockout implementation must be tested for **transaction rollback** and **concurrent login attempts** before production use.

### 8️⃣ API Rate Limiting with Bucket4j

Bucket4j is integrated into a custom request filter.

**Example configuration:**

```properties
security.rate-limit.general.capacity=100
security.rate-limit.general.refill-minutes=1
security.rate-limit.login.capacity=10
security.rate-limit.login.refill-minutes=1
```

- Login endpoint: `POST /api/v1/auth/login`
- Login-specific limit and general limit selected by filter
- When bucket is exhausted → **`429 Too Many Requests`**

**⚠️ Current implementation considerations:**

- 🧠 Rate-limit buckets are stored in **application memory**
- 🖥️ Limits maintained separately by **client IP and endpoint**
- 🔄 Bucket state is **lost on restart**
- 🌐 Multiple instances **do not share** limits
- ⏱️ Interval refill is **not necessarily a rolling one-minute window**
- 🧹 Unused bucket entries need an **eviction strategy** to prevent memory growth

### 9️⃣ Super-Admin Bootstrap Control

The initial super-admin can be provisioned using a Spring `CommandLineRunner` bean.

**Bootstrap is controlled through:**

```properties
app.bootstrap-super-admin.enabled=false
```

When `false`, a bean annotated with `@ConditionalOnProperty` is **not created**.

**For authorized, one-time setup:**

```bash
java -jar secure-auth-api.jar \
  --app.bootstrap-super-admin.enabled=true
```

> 🔒 After initial provisioning, start the application **without** the override. Production credentials must be supplied through a secure configuration mechanism. The bootstrap process must check whether the account already exists and must **not** create a public super-admin registration endpoint.

---

## 👥 User Roles and Permissions

| Role | Intended Access |
|------|-----------------|
| 🟢 `ROLE_USER` | Normal user functionality |
| 🟠 `ROLE_ADMIN` | Administrative functionality permitted by policy |
| 🔴 `ROLE_SUPER_ADMIN` | Privileged administrative operations |
| 🟣 `Primary / Backup Super-Admin` | Special super-admin classifications |

> 🛡️ **Important:** The exact permissions should be enforced by Spring Security and **server-side** authorization checks, **not** by trusting a role sent by a client.

---

## 🌐 API Endpoints

| Method | Endpoint | Purpose | Access |
|--------|----------|---------|--------|
| `POST` | `/api/v1/auth/register` | Register a user | 🌐 Public |
| `POST` | `/api/v1/auth/login` | Authenticate and obtain a JWT | 🌐 Public |
| `POST` | `/api/v1/auth/logout` | Revoke the current token | 🔐 Authenticated |
| `POST` | `/api/v1/auth/forgot-password` | Initiate password recovery | 🌐 Public, rate-limited |
| `POST` | `/api/v1/auth/reset-password` | Reset password using valid reset token | 🌐 Token-based |
| `POST` | `/api/v1/auth/admin-registration` | Register an invited admin | 🎫 Invitation token required |

> 📌 Other endpoints (user blocking, admin/super-admin dashboards) are subject to routes implemented in current controllers.

### 📊 HTTP Status Codes

| Status | Meaning |
|--------|---------|
| `200 OK` | Request completed successfully |
| `201 Created` | Resource created successfully |
| `400 Bad Request` | Invalid request or validation error |
| `401 Unauthorized` | Missing or invalid authentication |
| `403 Forbidden` | Authenticated user lacks permission |
| `423 Locked` | Account is locked |
| `429 Too Many Requests` | Rate limit exceeded |
| `500 Internal Server Error` | Unexpected server error |

---

## 🔄 Authentication Flow

```text
1. User registers through the registration API
   └─► Server hashes the password before saving it

2. User logs in with their credentials
   └─► Server verifies the password

3. Server issues a signed JWT with configured claims
   └─► Client sends token in subsequent requests:
       Authorization: Bearer <access-token>

4. Spring Security validates token and authorizes the request

5. During logout, the token is revoked
   └─► Subsequent requests using that token are rejected
```

---

## 🔐 Security Implementation

### 🗝️ Password Security

- ✅ Store password **hashes**, never plaintext
- ✅ Use adaptive encoders (BCrypt)
- ❌ Never log passwords or include them in error responses

### 🎫 JWT Security

- ✅ Keep signing keys outside source control
- ✅ Validate signature, expiration, and required claims
- ✅ Reject revoked tokens
- ✅ Keep token lifetimes short
- ❌ Never trust client-provided role values

### 🛡️ Authorization

- ✅ Use Spring Security authorization rules
- ✅ Enforce super-admin-only operations on the server
- ✅ Prevent users from changing their own role
- ✅ Protect admin invitations and user-blocking operations

### 🚦 Abuse Prevention

- ✅ Use Bucket4j to limit excessive requests
- ✅ Apply stricter limits to login, registration, and password recovery
- ✅ Keep account lockout separate from IP-based rate limiting
- ✅ Return consistent error responses without exposing internal details

### 🔑 Configuration and Secrets

- ✅ Use environment variables or a secrets manager
- ✅ Disable super-admin bootstrap by default
- ✅ Disable unnecessary production debug logging
- ✅ Use HTTPS in production

---

## ⚙️ Configuration

**Example development configuration:**

```properties
# ─── Database configuration ────────────────────────
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

# ─── Login protection ──────────────────────────────
security.login.max-failed-attempts=5
security.login.lock-duration-minutes=240

# ─── General rate limit ────────────────────────────
security.rate-limit.general.capacity=100
security.rate-limit.general.refill-minutes=1

# ─── Login rate limit ──────────────────────────────
security.rate-limit.login.capacity=10
security.rate-limit.login.refill-minutes=1

# ─── Super-admin bootstrap ─────────────────────────
app.bootstrap-super-admin.enabled=false
```

> ⚠️ Configure the appropriate MySQL driver and JPA settings according to your project.
> 🚫 **Do not commit production secrets or real credentials to Git.**

---

## 🗄️ Database

The application uses **MySQL** and **Spring Data JPA**.

**Tables may include:**

- 👤 User/account table
- 🎫 Revoked JWT token table
- 📧 Invitation or password-reset token storage

> 💡 **Recommendation:** Use database migrations for controlled schema changes in production. Do **not** rely solely on automatic schema updates.

---

## ▶️ Run the Application

### 📋 Prerequisites

- ☕ JDK compatible with the project
- 📦 Maven
- 🐬 MySQL
- 🔧 Git

### 🚀 Steps

**1. Clone the repository**

```bash
git clone <your-repository-url>
cd secure-auth-api
```

**2. Create the MySQL database**

```sql
CREATE DATABASE secure_auth_db;
```

**3. Configure the required environment variables**

```bash
export DB_URL=jdbc:mysql://localhost:3306/secure_auth_db
export DB_USERNAME=your_username
export DB_PASSWORD=your_password
```

**4. Build the project**

```bash
mvn clean package
```

**5. Run the application**

```bash
java -jar target/secure-auth-api-*.jar
```

**6. Test the APIs using Postman** 🧪

> ℹ️ Use the actual artifact name generated by Maven if it differs from the example.

---

## 🧪 Testing with Postman

### Recommended Manual Test Scenarios

| # | Test Scenario | Expected Result |
|---|---------------|-----------------|
| 1 | Register a new user | ✅ `201 Created` |
| 2 | Log in with valid credentials | ✅ `200 OK` + JWT |
| 3 | Repeated incorrect password | 🔒 Account locks after 5 attempts |
| 4 | Verify account-lockout behavior | ✅ Lock respected |
| 5 | Access protected endpoint with valid JWT | ✅ `200 OK` |
| 6 | Attempt access without a JWT | ❌ `401 Unauthorized` |
| 7 | Normal user accesses admin endpoint | ❌ `403 Forbidden` |
| 8 | Logout and reuse revoked token | ❌ `401 Unauthorized` |
| 9 | Test expired and invalid JWTs | ❌ `401 Unauthorized` |
| 10 | Exceed login rate limit | 🚦 `429 Too Many Requests` |
| 11 | Test invitation-token expiration & reuse | ❌ Rejected |
| 12 | Test password-reset token expiration & reuse | ❌ Rejected |
| 13 | Bootstrap runner disabled | ✅ No super-admin created |

> 🧪 Automated integration and security tests should be added for repeatable verification.

---

## ✅ Production Deployment Checklist

- [ ] 🔒 **HTTPS is enforced**
- [ ] 🔑 **Production secrets stored securely**
- [ ] 🎫 **JWT signing keys are strong and protected**
- [ ] 🔐 **Adaptive password encoder in use**
- [ ] ⏱️ **JWT expiration and revocation tested**
- [ ] 🎟️ **Reset & invitation tokens short-lived and single-use**
- [ ] 🚫 **Super-admin creation disabled during normal startup**
- [ ] 🛡️ **Administrative authorization tested**
- [ ] 🚦 **Appropriate rate limits on sensitive endpoints**
- [ ] 🌐 **Rate-limit behavior suitable for multiple instances**
- [ ] 🔄 **Account lockout handles concurrent requests**
- [ ] 🧹 **Input validation & safe error handling**
- [ ] 🗄️ **DB permissions follow least privilege**
- [ ] 🕵️ **Dependencies scanned for vulnerabilities**
- [ ] 📝 **Logs free of passwords, tokens, sensitive info**
- [ ] 💾 **Backups, monitoring, and incident-response ready**

---

## 🛠️ Known Limitations and Future Improvements

> The following items should be verified or implemented as applicable:

| Area | Improvement |
|------|-------------|
| 🚦 **Rate Limiting** | Persistent or distributed rate limiting for multiple instances |
| 🧹 **Buckets** | Eviction strategy and `Retry-After` support |
| 🎯 **Dedicated Limits** | Specific limits for registration and password recovery |
| 🔐 **Token Storage** | Secure persistent storage for reset & invitation tokens |
| 🧪 **Testing** | Automated security and authorization integration tests |
| 🗄️ **DB Migrations** | Database migrations and production schema management |
| 📝 **Audit** | Audit logging for administrative actions |
| 📊 **Monitoring** | Alerting for suspicious authentication activity |
| 🕵️ **Security** | Dependency scanning and penetration testing |
| 🌐 **Proxy** | Review forwarded client IP headers behind trusted proxy |

---

## ⚠️ Security Disclaimer

> 🛡️ **This project demonstrates common authentication and authorization mechanisms.**
>
> Listing a security feature **does not establish** that it is implemented correctly or is immune to vulnerabilities.
>
> The application **should undergo** code review, automated testing, dependency scanning, and security testing **before production use**.

---

<div align="center">

### 🔐 Built with Spring Boot & Spring Security

**⭐ Star this repository if you find it useful!**

</div>