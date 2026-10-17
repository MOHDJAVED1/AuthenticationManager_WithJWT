Secure Auth API — API Documentation

1. Project Overview

Project: Secure Auth API
Technology: Java 17, Spring Boot, Spring Security, JWT, Spring Data JPA, MySQL
Base URL: "http://localhost:8080"
API Version: "v1"
Content-Type: "application/json"

Features

- User registration
- Login using email and password
- JWT-based authentication
- Logout with JWT revocation
- Forgot password and reset password
- BCrypt password hashing
- Admin and normal user roles
- Admin can enable or disable user accounts
- Protected APIs for authenticated users

«Implementation status: Registration, login, and JWT logout have been developed during this project. Password-reset APIs are under development. Admin management APIs are planned. Verify each endpoint against the current code before treating this document as the final API contract.»

---

2. Authentication Rules

Endpoint| Method| Authentication
"/api/v1/auth/register"| POST| Public
"/api/v1/auth/login"| POST| Public
"/api/v1/auth/logout"| POST| JWT required
"/api/v1/auth/forgot-password"| POST| Public
"/api/v1/auth/reset-password"| POST| Public + valid reset token
"/api/v1/admin/users"| GET| Admin only
"/api/v1/admin/users/{id}/status"| PATCH| Admin only

For protected APIs, send the JWT in the request header:

Authorization: Bearer <access-token>

Do not send the JWT in the URL or request body.

---

3. User Registration API

Endpoint

"POST /api/v1/auth/register"

Description

Registers a new user account. The server should assign "ROLE_USER" by default. Public registration must never allow users to assign themselves the admin role.

Request Payload

{
  "name": "Mohd Javed",
  "email": "javed@example.com",
  "password": "StrongPassword123"
}

Successful Response — "201 Created"

{
  "id": 1,
  "name": "Mohd Javed",
  "email": "javed@example.com",
  "message": "Registration successful"
}

The actual response status and field names depend on the existing controller implementation.

Possible Errors

- "400 Bad Request" — invalid request fields or password validation failure.
- "409 Conflict" — email already registered, if implemented with this status.

Security

- Hash passwords using BCrypt.
- Never return the password or password hash.
- Assign "ROLE_USER" on the server.

---

4. User Login API

Endpoint

"POST /api/v1/auth/login"

Description

Authenticates both admin and normal users. The server determines the user's role from the database.

Request Payload

{
  "email": "javed@example.com",
  "password": "StrongPassword123"
}

Successful Response

If the current controller returns a JWT as a string, the response may be a JSON string. A recommended response format is:

{
  "accessToken": "<jwt-token>",
  "tokenType": "Bearer",
  "expiresIn": 900
}

The token should contain the subject, unique token ID ("jti"), expiry, and role claim.

Possible Errors

- "400 Bad Request" — invalid request fields.
- "401 Unauthorized" — invalid email/password or disabled account.

Security

- Never store passwords in plain text.
- Use BCrypt to verify the password.
- Do not trust a role supplied in the request.
- Do not issue a token to a disabled user.

---

5. User Logout API

Endpoint

"POST /api/v1/auth/logout"

Description

Logs out the current user by revoking the presented JWT.

Request Headers

Authorization: Bearer <access-token>

Request Body

No body required.

Successful Response — "200 OK"

{
  "message": "Logout successful"
}

The response shape may differ if the current controller returns a plain string.

Possible Errors

- "401 Unauthorized" — missing, expired, invalid, or revoked token.
- "403 Forbidden" — access denied by an authorization rule, if applicable.

Expected Behaviour

1. The server validates the JWT.
2. It reads the JWT's "jti" and expiration time.
3. It stores the token ID in the "revoked_tokens" table.
4. Spring Security checks revocation on protected requests.
5. Subsequent requests with that token are rejected.

«Saving a token ID in MySQL alone does not invalidate a JWT. The JWT validation process must check the revocation table.»

---

6. Forgot Password API

Endpoint

"POST /api/v1/auth/forgot-password"

Description

Requests a password-reset token for a registered email address.

The initial learning version generates a token and returns it in the response. Later, the token should be delivered through email.

Request Payload

{
  "email": "javed@example.com"
}

Development Response

{
  "message": "Reset token generated for development",
  "resetToken": "<temporary-reset-token>"
}

The current planned implementation stores tokens in memory and sets a 10-minute expiry.

Production Response Recommendation

{
  "message": "If the account exists, password reset instructions will be sent."
}

Possible Errors

- "400 Bad Request" — missing or invalid email.

In production, avoid revealing whether the email address is registered.

Security

- Generate a cryptographically secure reset token.
- Expire the token after a short period.
- Make the token single-use.
- Never return the reset token in production responses.
- Add rate limiting to prevent abuse.

---

7. Reset Password API

Endpoint

"POST /api/v1/auth/reset-password"

Description

Changes the password after validating the reset token.

Request Payload

{
  "email": "javed@example.com",
  "resetToken": "<temporary-reset-token>",
  "newPassword": "NewStrongPassword123"
}

Successful Response — "200 OK"

{
  "message": "Password reset successful"
}

Possible Errors

- "400 Bad Request" — invalid, expired, or already-used reset token.
- "400 Bad Request" — invalid password fields.

Security

- Verify the reset token before changing the password.
- Ensure the token belongs to the same account.
- Hash the new password with BCrypt.
- Delete or invalidate the reset token after successful use.
- Revoke existing sessions or tokens after a password reset.

«The reset token must be verified even though the endpoint is public. Public access means no login is required; it does not mean anyone can change any user's password.»

---

8. Admin Login

Endpoint

"POST /api/v1/auth/login"

Description

Admins and normal users use the same login API. The backend identifies the role from the user's database record.

Admin Login Request

{
  "email": "admin@example.com",
  "password": "AdminStrongPassword123"
}

Example JWT Claims

{
  "sub": "admin@example.com",
  "jti": "<unique-token-id>",
  "role": "ROLE_ADMIN"
}

A normal user's role would be "ROLE_USER".

These are illustrative decoded claims, not a complete signed JWT.

Security

- Create the initial admin through a trusted setup process.
- Never allow public registration to assign "ROLE_ADMIN".
- Validate the token signature and expiration.
- Configure Spring Security to map the role claim to authorities.

---

9. Admin — List Users API

Status: Planned

Endpoint

"GET /api/v1/admin/users"

Description

Allows an authenticated admin to view user accounts.

Request Headers

Authorization: Bearer <admin-access-token>

Request Body

No body required.

Example Response — "200 OK"

[
  {
    "id": 1,
    "name": "Mohd Javed",
    "email": "javed@example.com",
    "role": "ROLE_USER",
    "enabled": true
  },
  {
    "id": 2,
    "name": "Test User",
    "email": "test@example.com",
    "role": "ROLE_USER",
    "enabled": false
  }
]

Possible Errors

- "401 Unauthorized" — missing or invalid JWT.
- "403 Forbidden" — authenticated user is not an admin.

Security

Never expose password hashes, reset tokens, or other sensitive information in the user list.

---

10. Admin — Enable or Disable User API

Status: Planned

Endpoint

"PATCH /api/v1/admin/users/{id}/status"

Example:

"PATCH /api/v1/admin/users/2/status"

Description

Allows an administrator to enable or disable a user account.

Request Headers

Authorization: Bearer <admin-access-token>
Content-Type: application/json

Request Payload — Disable User

{
  "enabled": false
}

Successful Response — "200 OK"

{
  "id": 2,
  "enabled": false,
  "message": "User disabled successfully"
}

Request Payload — Enable User

{
  "enabled": true
}

Successful Response — "200 OK"

{
  "id": 2,
  "enabled": true,
  "message": "User enabled successfully"
}

Possible Errors

- "400 Bad Request" — invalid status or prohibited account operation.
- "401 Unauthorized" — missing or invalid JWT.
- "403 Forbidden" — caller is not an admin.
- "404 Not Found" — user ID does not exist.

Expected Behaviour

When an account is disabled:

1. The user cannot log in.
2. Existing JWTs for that user must no longer grant access to protected APIs.
3. The user should receive "401 Unauthorized" when attempting protected access.

Checking "enabled" only during login is insufficient. Existing JWTs must also be checked against current account status, or invalidated through another reliable mechanism.

---

11. Common HTTP Status Codes

Status| Meaning
"200 OK"| Request successful
"201 Created"| Resource created
"400 Bad Request"| Invalid request data
"401 Unauthorized"| Authentication failed or token invalid
"403 Forbidden"| Authenticated user lacks permission
"404 Not Found"| Resource not found
"409 Conflict"| Resource conflicts with existing data
"500 Internal Server Error"| Unexpected server error

---

12. Implementation Checklist

- [ ] Registration with BCrypt password hashing
- [ ] Login and JWT generation
- [ ] JWT validation using Spring Security
- [ ] Logout and revoked-token validation
- [ ] Forgot-password token generation
- [ ] Reset-password token validation and single-use enforcement
- [ ] Email delivery for password-reset instructions
- [ ] Admin account setup
- [ ] Role-based authorization
- [ ] Admin user listing
- [ ] Admin enable/disable user
- [ ] Reject disabled users on protected requests
- [ ] Revoke existing tokens after password reset
- [ ] Standardize error responses
- [ ] Rate-limit authentication endpoints
- [ ] Use HTTPS in deployed environments

---

13. Important Notes

This document describes the current learning project and proposed future endpoints. The admin APIs are not implemented merely because they are documented. Request/response examples should be synchronized with the actual Java DTOs and controller methods.

Before production deployment, add email verification, secure reset-token persistence, rate limiting, consistent error handling, audit logging for admin actions, and appropriate token/session invalidation.