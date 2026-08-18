<p align="center">
  <img src="https://capsule-render.vercel.app/api?type=waving&color=0:22d3ee,100:8b5cf6&height=220&section=header&text=EntityKart%20Monolith&fontSize=58&fontColor=ffffff&animation=fadeIn&fontAlignY=38&desc=Full-Stack%20Spring%20Boot%203%20%2B%20AngularJS%20E-Commerce%20Platform&descAlignY=60&descAlign=50" width="100%">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java_25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white">
  <img src="https://img.shields.io/badge/Spring_Boot_3.3.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white">
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white">
  <img src="https://img.shields.io/badge/AngularJS_1.8-E23237?style=for-the-badge&logo=angular&logoColor=white">
  <img src="https://img.shields.io/badge/Flyway-CC292B?style=for-the-badge&logo=flyway&logoColor=white">
  <img src="https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white">
  <img src="https://img.shields.io/badge/Cloudinary-3448C5?style=for-the-badge&logo=cloudinary&logoColor=white">
  <img src="https://img.shields.io/badge/Razorpay-02042B?style=for-the-badge&logo=razorpay&logoColor=white">
</p>

<p align="center">
  <img src="https://readme-typing-svg.demolab.com?font=Fira+Code&weight=600&size=22&duration=3000&pause=500&color=22D3EE&center=true&vCenter=true&width=700&lines=Spring+Boot+3.3.4+Monolith+Architecture;JWT+Auth+%2B+XSRF+Cookie+Security;Flyway+DB+Migration+%2B+Auto-Seeding;Razorpay+%2B+Authorize.Net+Payments;Cloudinary+Image+Upload;Twilio+SMS+%2B+Gmail+SMTP;GraphQL+%2B+REST+API;AngularJS+1.8+Frontend" alt="Typing SVG">
</p>

<p align="center">
  <img src="https://img.shields.io/github/stars/Sadique721/monolith?style=for-the-badge&color=22d3ee">
  <img src="https://img.shields.io/github/forks/Sadique721/monolith?style=for-the-badge&color=8b5cf6">
  <img src="https://img.shields.io/badge/Build-Passing-brightgreen?style=for-the-badge&logo=gradle">
  <img src="https://img.shields.io/badge/License-MIT-10b981?style=for-the-badge">
</p>

---

## 👨‍💻 Author & Architect

<table>
<tr>
<td align="center" width="160">
  <a href="https://github.com/Sadique721">
    <img src="https://avatars.githubusercontent.com/Sadique721" width="110" style="border-radius:50%"><br>
    <b>Md Sadique Amin</b><br>
    <sub>Software Engineer & Full-Stack Architect</sub>
  </a>
</td>
<td>

**Md Sadique Amin** — Software Engineer, Telecom & Full-Stack Cloud Architect, AI Systems Developer.

- 🔗 GitHub: [@Sadique721](https://github.com/Sadique721)
- 📧 Email: mdsadiqueamin721786@gmail.com
- 🏗️ Built: Enterprise BSS-OSS Telecom Suite, Diameter Protocol Engine, Angular & Flutter Apps, MSA AI Ecosystem

</td>
</tr>
</table>

---

## 📖 Project Overview

**EntityKart Monolith** is a production-grade, full-stack **e-commerce platform** built with **Spring Boot 3.3.4** (backend) and **AngularJS 1.8** (frontend). Originally architected as 9 independent cloud microservices, it has been consolidated into a single deployable monolith using classic layered packaging — making it easier to develop, test, and deploy without complex infrastructure.

### 🏛️ Architecture: Before → After

```
BEFORE (Microservices)                    AFTER (Monolith)
─────────────────────────────────         ──────────────────────────────
┌─user-service─────────┐                  ┌─── entitykart-monolith ────┐
├─product-service───────┤                  │  ┌─entity/               │
├─cart-service──────────┤   ──────────►   │  ├─repository/           │
├─order-service─────────┤                  │  ├─service/              │
├─payment-service───────┤   Single JAR     │  ├─controller/           │
├─review-service────────┤                  │  ├─dto/                  │
├─notification-service──┤   No Kafka       │  ├─mapper/               │
├─wishlist-service──────┤   No Eureka      │  ├─security/             │
└─return-service────────┘   No API Gateway │  └─config/               │
  + Kafka + Eureka                          └──────── port 8080 ────────┘
  + API Gateway                             + AngularJS frontend :3000
```

---

## ✨ Features

| Category | Features |
|----------|----------|
| 🔐 **Authentication** | JWT Access + Refresh Token, XSRF-Cookie CSRF protection, HttpOnly cookies |
| 👤 **User Management** | Registration, Login, Profile, Address CRUD, Role-based (ADMIN/SELLER/USER) |
| 📦 **Product Catalog** | 1000+ products, 12 categories, 36 sub-categories, Cloudinary image upload |
| 🛒 **Cart & Orders** | Add to cart, Checkout, Order tracking, Status updates |
| 💳 **Payments** | Razorpay (UPI/Card), Authorize.Net (International), Order payment history |
| ⭐ **Reviews** | Product reviews, ratings, admin moderation, stats & distribution |
| ❤️ **Wishlist** | Add/remove products, persistent wishlist |
| 🔄 **Returns** | Return requests, status tracking, admin approval flow |
| 🔔 **Notifications** | In-app notifications, SMS via Twilio, Email via Gmail SMTP |
| 📊 **Admin Dashboard** | User stats, product analytics, order management, Excel/Word export |
| 📡 **GraphQL** | Product queries via GraphQL endpoint (`/graphql`) |
| 🗄️ **DB Migrations** | 19 Flyway migrations, auto-seeding of admin, categories, 1000 products |
| 🗃️ **Exports** | Excel + Word export for orders, products, users, payments, reviews, returns |

---

## 🗂️ Project Structure

```
entitykart-monolith/
├── src/main/java/com/entitykart/monolith/
│   ├── config/                    # App config, seeders, security config
│   │   ├── SecurityConfig.java
│   │   ├── UserDatabaseSeeder.java
│   │   └── ProductDatabaseSeeder.java
│   ├── controller/                # REST Controllers (15 controllers)
│   │   ├── AuthController.java    # /api/auth/*
│   │   ├── UserController.java    # /api/users/*
│   │   ├── ProductController.java # /api/products/*
│   │   ├── CartController.java    # /api/cart/*
│   │   ├── OrderController.java   # /api/orders/*
│   │   ├── PaymentController.java # /api/payments/*
│   │   ├── ReviewController.java  # /api/reviews/*
│   │   ├── WishlistController.java
│   │   ├── ReturnController.java
│   │   ├── NotificationController.java
│   │   ├── CategoryController.java
│   │   ├── AddressController.java
│   │   ├── AdminExportController.java
│   │   └── GraphQLProductController.java
│   ├── service/                   # Business Logic
│   ├── repository/                # Spring Data JPA Repositories
│   ├── entity/                    # JPA Entities
│   ├── dto/                       # Request/Response DTOs
│   ├── mapper/                    # Entity ↔ DTO Mappers
│   ├── security/                  # JWT Filter, Auth utilities
│   └── exception/                 # Global Exception Handler
├── src/main/resources/
│   ├── application.yml            # All config (DB, JWT, Mail, Payment, etc.)
│   ├── db/migration/              # 19 Flyway SQL migrations (V1–V19)
│   └── graphql/                   # GraphQL schema
├── frontend/                      # AngularJS 1.8 Frontend
│   ├── index.html                 # Single Page App shell
│   ├── js/
│   │   ├── app.js                 # Angular module + routing + API_BASE
│   │   ├── controllers/           # Page controllers (admin, product, auth...)
│   │   ├── services/              # API, Auth, Cart, Order services
│   │   └── directives/            # Custom Angular directives
│   ├── css/                       # Styles
│   ├── views/                     # HTML partials
│   ├── package.json               # lite-server dev server
│   └── bs-config.json             # BrowserSync config
├── build.gradle.kts               # Gradle build file
├── settings.gradle.kts
├── gradlew / gradlew.bat
└── README.md
```

---

## ⚙️ Tech Stack

| Layer | Technology |
|-------|------------|
| **Backend** | Java 25, Spring Boot 3.3.4, Spring Data JPA, Spring Security |
| **Database** | MySQL 8 (Local) / Aiven Cloud MySQL |
| **Migrations** | Flyway (19 versioned migrations) |
| **Auth** | JWT (JJWT), HttpOnly Cookies, XSRF-Token CSRF |
| **Frontend** | AngularJS 1.8, lite-server, Bootstrap |
| **Payments** | Razorpay, Authorize.Net (Sandbox) |
| **Media** | Cloudinary (Image Upload & Management) |
| **Email** | Gmail SMTP (Spring Mail) |
| **SMS** | Twilio |
| **API** | REST + GraphQL |
| **Build** | Gradle 9.3 |
| **Export** | Apache POI (Excel), Apache POI (Word/DOCX) |

---

## 🚀 Quick Start

### Prerequisites
- Java 17+ (tested on JDK 25)
- MySQL 8.x running locally
- Node.js + npm (for frontend)
- Git

### 1️⃣ Clone the repository

```bash
git clone https://github.com/Sadique721/monolith.git
cd monolith
```

### 2️⃣ Configure Database

Create database in MySQL:
```sql
CREATE DATABASE entitykart_database;
```

Edit `src/main/resources/application.yml` — datasource section:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/entitykart_database?useSSL=false&serverTimezone=UTC
    username: root
    password: YOUR_PASSWORD
```

### 3️⃣ Start Backend

```bash
# Windows
set JAVA_HOME=C:\Program Files\Java\jdk-25.0.4
.\gradlew bootRun

# Linux/Mac
export JAVA_HOME=/usr/lib/jvm/java-25
./gradlew bootRun
```

Backend starts at: **http://localhost:8080**

On first run, Flyway automatically:
- Runs 19 DB migration scripts
- Seeds admin user, 12 categories, 36 sub-categories, 1000 products

### 4️⃣ Start Frontend

```bash
cd frontend
npm install
npm start
```

Frontend opens at: **http://localhost:3000**

---

## 🔑 Default Credentials

| Role | Email | Password |
|------|-------|----------|
| **ADMIN** | mdsadiqueamin721721@gmail.com | `Amin@123` |

---

## 🌐 API Endpoints Reference

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/users/login` | Login → JWT cookie |
| POST | `/api/users/register` | Register new user |
| POST | `/api/auth/refresh` | Refresh access token |
| POST | `/api/auth/logout` | Logout → clear cookies |

### Products
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/products` | List all products (paginated) |
| GET | `/api/products/{id}` | Product details |
| POST | `/api/products` | Create product (SELLER/ADMIN) |
| PUT | `/api/products/{id}` | Update product |

### Orders
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/orders` | Place new order |
| GET | `/api/orders/user/{id}` | User's orders |
| GET | `/api/orders/all` | All orders (ADMIN) |

### Admin
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/users/all` | All users (ADMIN) |
| GET | `/api/admin/export/orders/excel` | Export orders → Excel |
| GET | `/api/admin/export/products/word` | Export products → Word |
| GET | `/api/reviews/admin/stats` | Review statistics |

### GraphQL
```
POST /graphql
```
```graphql
query {
  products(page: 0, size: 10) {
    content {
      productId
      productName
      price
      brand
    }
  }
}
```

---

## 🔐 Security Architecture

```
Browser Request
      │
      ▼
┌─────────────────────────────────────┐
│     JwtAuthenticationFilter          │
│  1. Read ek_access_token cookie      │
│  2. Validate JWT signature           │
│  3. XSRF-Token check (POST/PUT/etc) │
│  4. Set SecurityContext             │
└─────────────────────────────────────┘
      │
      ▼
┌─────────────────────────────────────┐
│      SecurityConfig                  │
│  • /api/auth/** → Public            │
│  • /api/products (GET) → Public     │
│  • /api/admin/** → ADMIN only       │
│  • All others → Authenticated       │
└─────────────────────────────────────┘
```

---

## ☁️ Aiven Cloud MySQL (Optional)

To use Aiven Cloud instead of local MySQL:

1. **Whitelist your IP** in Aiven Console → Networking → Add IP
2. **Uncomment** Aiven config in `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://entitkart-mdsadiqueamin721786-entitykart.a.aivencloud.com:20904/defaultdb?useSSL=true&requireSSL=true&verifyServerCertificate=false&serverTimezone=UTC
    username: avnadmin
    password: YOUR_AIVEN_PASSWORD
```

---

## 📋 DB Migration Summary

| Version | Migration |
|---------|-----------|
| V1 | Create users table |
| V2 | Create categories & subcategories |
| V3 | Create products table |
| V4-V6 | Create cart, addresses, indexes |
| V7-V9 | Create orders, order items, indexes |
| V10-V11 | Create payments, indexes |
| V12-V13 | Create wishlist, indexes |
| V14-V15 | Create reviews, indexes |
| V16-V17 | Create returns, indexes |
| V18-V19 | Create notifications, indexes |

---

## 🧪 Verification Results (11/11 Passing)

```
✅ Products API         → 1000 products
✅ Categories API       → 12 categories
✅ Login (Admin JWT)    → Token + Role verified
✅ Frontend (port 3000) → AngularJS app live
✅ Admin: Users         → 200 OK
✅ Admin: Orders        → 200 OK
✅ Admin: Payments      → 200 OK
✅ Admin: Review Stats  → 200 OK
✅ Admin: Distribution  → 200 OK
✅ Admin: Excel Export  → .xlsx file download
✅ CORS Headers         → Origin: http://localhost:3000
```

---

## 📦 Third-Party Integrations

| Service | Purpose | Status |
|---------|---------|--------|
| **Gmail SMTP** | Transactional emails | ✅ Configured |
| **Cloudinary** | Product image upload/CDN | ✅ Configured |
| **Razorpay** | Indian payment gateway | ✅ Sandbox |
| **Authorize.Net** | International payments | ✅ Sandbox |
| **Twilio** | SMS notifications | ✅ Configured |
| **Aiven MySQL** | Cloud DB (optional) | ⚙️ IP whitelist needed |

---

## 📄 License

MIT © 2026 [Md Sadique Amin](https://github.com/Sadique721)

---

<p align="center">
  <img src="https://capsule-render.vercel.app/api?type=waving&color=0:8b5cf6,100:22d3ee&height=120&section=footer" width="100%">
</p>

<p align="center">
  <b>Built with ❤️ by <a href="https://github.com/Sadique721">Md Sadique Amin</a></b><br>
  <sub>Software Engineer · Full-Stack Architect · AI Systems Developer</sub>
</p>
