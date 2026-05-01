# Service Marketplace

A university student marketplace for services, built with Spring Boot and React.

## Prerequisites

- Java 17
- Maven 3.8+
- Node.js 18+
- npm 9+
- MongoDB (local instance or MongoDB Atlas)

## Local Setup

### 1. Clone the repository

git clone https://github.com/your-org/Service-Marketplace.git
cd Service-Marketplace

### 2. Configure environment variables

Copy the example env file and fill in your values:

cp .env.example .env

### 3. Run the backend

cd packages/backend
mvn spring-boot:run

The API will start on http://localhost:8080

### 4. Run the frontend

cd packages/frontend
npm install --legacy-peer-deps
npm run dev

The app will start on http://localhost:5173

## Project Structure

packages/
├── backend/   # Spring Boot API
└── frontend/  # React + Vite client

## Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md) for branch naming, commit message conventions, and PR rules.