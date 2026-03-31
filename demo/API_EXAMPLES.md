# MiPyme API Examples

Here are the curl commands to interact with the API.

## 1. Register a Company (and Admin User)

This creates a new company tenant and its first administrator.

```bash
curl --location 'http://localhost:8080/api/companies/register' \
--header 'Content-Type: application/json' \
--data '{
    "businessName": "Mi Pyme SA",
    "tradeName": "Mi Pyme Store",
    "ssoCode": "MPSA02",
    "termsAccepted": true,
    "adminName": "Admin User",
    "adminEmail": "admin@mipyme.com",
    "adminPassword": "admin123"
}'
```

**Note:** The `ssoCode` ("MPSA02") must be unique and is used for login. Fields like CUIT, country, province, city, phone, companyEmail, and fiscalAddress can be configured later via the `PUT /api/companies/me` endpoint.

## 2. Register a New User (in an existing company)

To add more users (e.g., an employee) to the company created above.

```bash
curl --location 'http://localhost:8080/api/auth/register' \
--header 'Content-Type: application/json' \
--data '{
    "ssoCode": "MPSA02",
    "name": "Employee One",
    "email": "emp1@mipyme.com",
    "password": "emp123",
    "role": "EMPLOYEE"
}'
```

## 3. Login

Log in using the `ssoCode` and user credentials.

**Admin Login:**
```bash
curl --location 'http://localhost:8080/api/auth/login' \
--header 'Content-Type: application/json' \
--data '{
    "ssoCode": "MPSA02",
    "username": "admin@mipyme.com",
    "password": "admin123"
}'
```

**Employee Login:**
```bash
curl --location 'http://localhost:8080/api/auth/login' \
--header 'Content-Type: application/json' \
--data '{
    "ssoCode": "MPSA02",
    "username": "emp1@mipyme.com",
    "password": "emp123"
}'
```
