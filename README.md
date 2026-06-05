# Service Marketplace

PolyServices is a service marketplace platform built for Cal Poly students, allowing them to offer and book campus services such as tutoring, tech help, meal prep, photography, and more. Students can create listings, browse available services, book and pay through Stripe, leave reviews, and manage their accounts — all within a verified Cal Poly community. The platform includes an admin dashboard for moderation, a reporting system for guideline violations, and email notifications for bookings and account activity. For architecture and monorepo documentation, see [architecture.md](./docs/architecture.md).

## Prerequisites

- Java 17
- Maven 3.8+
- Node.js 18+
- npm 9+
- MongoDB (local instance or MongoDB Atlas)

## Local Setup

### 1. Clone the repository

git clone https://github.com/your-org/Service-Marketplace.git cd
Service-Marketplace

### 2. Configure environment variables

Copy the example env file and fill in your values:

cp .env.example .env

### 3. Run the backend

cd packages/backend/service-marketplace ./mvnw spring-boot:run

The API will start on http://localhost:8080

### 4. Run the frontend

cd packages/frontend npm install --legacy-peer-deps npm run dev

The app will start on http://localhost:5173

## Project Structure

packages/ ├── backend/ │ └── service-marketplace/ │ └──
src/main/java/com/ServiceMarketplace/service_marketplace/ │ ├──
config/ # Security and app configuration (JWT filter, BCrypt,
CORS) │ ├── controller/ # HTTP layer — maps requests to service
calls │ ├── dto/ # Data Transfer Objects — request/response
shapes (Lombok @Value/@Data) │ ├── exception/ # Custom
exceptions and global exception handler │ ├── model/ # MongoDB
document models (Lombok @Data) │ ├── repository/ # Spring Data
MongoDB repositories │ └── service/ # Business logic └──
frontend/ └── src/ ├── components/ # Shared UI components └──
pages/ # Route-level page components

## Code Conventions

### Backend

- Constructor injection with `final` fields across all layers
- Lombok `@Data` for mutable models, `@Value` for immutable DTOs
- Custom exceptions extend `RuntimeException` and are handled
  globally via `GlobalExceptionHandler`
- Environment variables follow UPPER_SNAKE_CASE naming (e.g.
  `SECRET_KEY`, `SENDER_EMAIL`)

### Frontend

- TypeScript throughout
- Shared components in `components/`, route-level pages in
  `pages/`

## Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md) for branch naming,
commit message conventions, and PR rules.
