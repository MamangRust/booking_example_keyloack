
# 🏨 Booking Management System

A robust **Room Booking System** built with **Spring Boot**, secured via **Keycloak**, and deployed using **Docker & Docker Compose** with **PostgreSQL** as the database.

---

## 🚀 Features

- 🔐 Authentication & Authorization using **Keycloak**
- 🧑 User Registration, Login, Email Verification, Password Reset
- 🏢 Room Management (CRUD)
- 📅 Room Booking with Check-in and Check-out
- 📦 Containerized with Docker and orchestrated using Docker Compose
- 💾 PostgreSQL database with auto-migrations
- 📄 REST API with JSON support
- 🧪 Health checks for services

---

## 📦 Tech Stack


| 🧱 Layer              | 🚀 Technology               |
|----------------------|-----------------------------|
| 🖥️ Backend           | Spring Boot (Java)          |
| 🔐 Auth              | Keycloak                    |
| 🗄️ Database          | PostgreSQL                  |
| 📦 Containerization  | Docker, Docker Compose      |
| 🧬 ORM               | JPA / Hibernate             |
| 📚 API Documentation | Swagger UI (SpringDoc)      |
---

## Running in App

```sh
make up
```

## Clean up App


```sh
make down
```