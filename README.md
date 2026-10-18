
# 🔐 Secure Auth API

A Spring Boot REST API for secure authentication, JWT-based authorization, password recovery, and role-based admin management.

## 📌 Overview

**Secure Auth API** is a backend application built using Java and Spring Boot. It demonstrates secure user authentication, password encryption, JWT validation, role-based access control, email notifications, and invitation-based admin registration.

The project is designed as a learning project to understand how authentication and authorization work in a real-world backend application.

## ✨ Features

### 🔑 Authentication & Authorization
- User registration and login
- JWT-based authentication
- Role-based access control (RBAC)
- Support for `ROLE_USER`, `ROLE_ADMIN`, and `ROLE_SUPER_ADMIN`
- Secure password hashing using BCrypt
- Logout with revoked-token validation

### 📧 Email Integration
- Gmail SMTP integration using Spring Boot Mail
- Email service reusable across application modules
- Email-based admin invitations
- Password-reset email notifications

### 🔄 Password Management
- Forgot password API
- Reset password using a secure token
- Token expiry validation
- Password updates using BCrypt hashing

### 👑 Admin Management
- Invitation-based admin registration
- Time-limited invitation tokens
- Single-use invitation validation
- Super Admin protected endpoints
- Optional direct admin creation by Super Admin

### 🛡️ Security
- Spring Security integration
- JWT role claim validation
- Protected endpoints based on user roles
- Public authentication endpoints with token-based business validation
- Request validation using Jakarta Validation

> Note: Keep only features that are implemented and tested in your current branch.

## 🧰 Technology Stack

| Technology | Purpose |
|---|---|
| Java 17 | Programming language |
| Spring Boot | Backend framework |
| Spring Security | Authentication and authorization |
| Spring Web | REST API development |
| Spring Data JPA | Database access |
| Hibernate | ORM |
| MySQL | Relational database |
| JWT | Token-based authentication |
| BCrypt | Password hashing |
| Spring Boot Mail | Email notifications |
| Maven | Build and dependency management |
| Postman | API testing |
| Lombok | Boilerplate reduction, where used |

## 🏗️ Application Architecture

```text
                  Client / Postman
                         |
                         v
                Spring Security
                         |
                         v
                    Controller
                         |
                         v
                     Service
                    /       \
                   v         v
             Repository   EmailService
                  |            |
                  v            v
                MySQL      Gmail SMTP
```

### Request Flow

1. The client sends an HTTP request.
2. Spring Security checks authentication and authorization rules.
3. The controller receives and validates the request.
4. The service executes business logic.
5. The repository interacts with MySQL when required.
6. EmailService sends email notifications when required.
7. The API returns an appropriate HTTP response.

## 📋 Prerequisites

Before running the project, install:

- JDK 17 or the JDK version configured in the project
- Maven
- MySQL Server
- Postman
- Gmail account with 2-Step Verification and an App Password for SMTP

## ⚙️ Configuration

### 1. Create the database

Execute the following SQL statement in MySQL:

```sql
CREATE DATABASE secure_auth_db;
```

### 2. Configure application.properties

Update `src/main/resources/application.properties` with your local database and SMTP configuration.

Example:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/secure_auth_db
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_APP_PASSWORD}

spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000
```

Configure the JWT secret using the property name expected by your existing JWT configuration.

**Important:** Never commit database passwords, Gmail App Passwords, JWT secrets, or other credentials to GitHub. Use environment variables or a secret manager.

## 🚀 Run the Application

Open a terminal in the project root directory.

### Build the project

```bash
mvn clean package
```

### Start the application

```bash
mvn spring-boot:run
```

Default local base URL:

```text
http://localhost:8080
```

Check your application configuration if a different port is configured.

## 🔗 API Endpoints

The following table describes the intended API structure. Confirm the exact mappings in your controllers.

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Register a normal user | Public |
| POST | `/api/v1/auth/login` | Authenticate a user | Public |
| POST | `/api/v1/auth/logout` | Logout and revoke token | Authenticated |
| POST | `/api/v1/auth/forgot-password` | Request password-reset instructions | Public |
| POST | `/api/v1/auth/reset-password` | Reset password using token | Public + valid reset token |
| POST | `/api/v1/auth/admin-registration` | Register an invited admin | Public + valid invitation token |
| GET | `/api/v1/super-admin/dashboard` | Access Super Admin dashboard | Super Admin |
| POST | `/api/v1/super-admin/admin-invitations` | Create an admin invitation | Super Admin |
| POST | `/api/v1/super-admin/admins` | Directly create an admin, if implemented | Super Admin |

Public routes should still validate their required reset or invitation tokens. Public registration must not allow users to assign themselves administrative roles.

## 🧪 Testing with Postman

### Authentication APIs

1. Start the Spring Boot application.
2. Open Postman.
3. Create a request using the appropriate HTTP method.
4. Set the request body to **Body → raw → JSON**.
5. Use **No Auth** for public endpoints.
6. Use a valid Bearer JWT for protected endpoints.

### Admin Invitation Flow

1. Authenticate as a Super Admin.
2. Call the admin invitation API with the invited user's email.
3. Receive the invitation email.
4. Import the supplied cURL command using **Postman → Import → Raw text**.
5. Verify the registration request and submit it.
6. Confirm that the admin account is created with `ROLE_ADMIN`.

### Forgot Password Flow

1. Call the forgot-password API with the account email.
2. Check the email for the password-reset request.
3. Import the supplied cURL command into Postman.
4. Set the desired new password.
5. Submit the reset request.
6. Log in using the updated password.

Never share real reset tokens, invitation tokens, or passwords in screenshots or public repositories.

## 🔐 Role-Based Access Control

| Role | Access |
|---|---|
| `ROLE_USER` | Normal user APIs |
| `ROLE_ADMIN` | Admin APIs and permitted user-level functionality |
| `ROLE_SUPER_ADMIN` | Super Admin APIs and permitted administrative functionality |

The exact access rules depend on the matchers and method-level security configured in Spring Security.

## 🛠️ Troubleshooting

### 401 Unauthorized
- Check the endpoint URL and HTTP method.
- Select No Auth for public endpoints.
- Verify that the endpoint is included in `permitAll()`.
- Check custom JWT filters and token validation.

### 403 Forbidden
- Verify the authenticated user's role.
- Check JWT role claim mapping.
- Review `hasRole()` and `hasAuthority()` configuration.

### Email not received
- Verify Gmail SMTP settings.
- Use a Gmail App Password rather than the normal account password.
- Check application logs and the spam folder.

### Database connection failure
- Confirm MySQL is running.
- Verify database name and credentials.
- Check the MySQL driver dependency.

## 🔒 Security Best Practices

- Store passwords using BCrypt.
- Keep JWT signing secrets and credentials outside source control.
- Use HTTPS outside local development.
- Use random, expiring, single-use reset and invitation tokens.
- Store token hashes where appropriate.
- Never put passwords in URLs.
- Avoid exposing raw tokens in application logs.
- Use generic forgot-password responses to avoid revealing whether an account exists.
- Add rate limiting to login and password-recovery endpoints.
- Use concurrency-safe token consumption for production.
- Require password setup or change for newly provisioned admin accounts.

## 🗺️ Future Improvements

- Automated unit and integration tests
- Global exception handling
- API documentation using OpenAPI/Swagger
- Rate limiting and account lockout
- Email verification
- Password-change notification emails
- Production deployment with HTTPS and managed secrets
- CI/CD integration

## 👨‍💻 Author

**Mohd Javed**

Java | Spring Boot | REST APIs | Spring Security | MySQL

## 📄 License

No license has been specified yet. Add a suitable `LICENSE` file before distributing the project publicly.
