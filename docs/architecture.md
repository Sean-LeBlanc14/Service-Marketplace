# Architecture Overview

Service Marketplace is a full-stack monorepo. The root of the repository is managed with npm workspaces and contains two packages under `packages/`:

```
Service-Marketplace/
├── packages/
│   ├── backend/          # Spring Boot REST API + WebSocket server
│   └── frontend/         # React + TypeScript SPA
├── .env.example          # Environment variable template
├── package.json          # npm workspace root (shared scripts)
└── README.md
```

---

## Backend

**Path:** `packages/backend/service-marketplace/`
**Stack:** Spring Boot 4.0.6, Java 17, Maven, MongoDB Atlas

The backend exposes a REST API on port 8080 and a WebSocket endpoint for real-time messaging. It follows a strict layered architecture: requests enter through controllers, all business logic lives in services, and data access is handled exclusively through repositories.

### Package Structure

```
src/main/java/com/ServiceMarketplace/service_marketplace/
├── config/
├── controller/
├── dto/
├── exception/
├── model/
├── repository/
└── service/
```

### config/

Spring configuration and cross-cutting concerns. Nothing in here contains business logic.

| File | Responsibility |
|---|---|
| `SecurityConfig.java` | Spring Security rules, CORS policy, stateless JWT session |
| `JwtAuthenticationFilter.java` | Intercepts every request, validates JWT, populates `SecurityContext` |
| `ApplicationConfig.java` | `@Bean` definitions — `UserDetailsService`, `PasswordEncoder`, `AuthenticationManager` |
| `WebSocketConfig.java` | Registers the STOMP message broker endpoint (`/ws`) |
| `WebSocketAuthInterceptor.java` | Validates the JWT on WebSocket handshake |

### controller/

HTTP layer only. Controllers map endpoints to service calls and return `ResponseEntity` wrappers around response DTOs. They never call repositories or contain conditional business logic.

| Controller | Endpoints |
|---|---|
| `AuthController` | Register, login, logout |
| `UserController` | Profile read/update, password change, account deletion |
| `ServiceController` | Service listing CRUD, search |
| `BookingController` | Create, confirm, cancel, reject bookings; submit review |
| `PaymentController` | Stripe setup intent, payment intent, Connect onboarding, webhook |
| `ConversationController` | Start conversation, send message, list conversations |
| `VerificationController` | Send and verify email codes |
| `ReportController` | Submit user/service reports |
| `SupportController` | Submit support tickets |

### service/

All business logic and all database access go here. Services are injected into controllers via constructor injection.

| Service | Responsibility |
|---|---|
| `UserService` | Registration, profile management, authentication helpers |
| `JwtService` | Token generation and validation |
| `BookingService` | Booking state machine, cancellation, refund orchestration |
| `BookingTokenService` | Time-limited email tokens for booking confirmation flows |
| `PaymentService` | Stripe payment intents, setup intents, Connect onboarding, refunds, webhook handling |
| `ServiceService` | Service listing CRUD and search |
| `ConversationService` | Messaging, conversation history, unread counts |
| `NotificationService` | In-app notification dispatch via WebSocket |
| `EmailService` | Transactional emails via SendGrid SMTP (Thymeleaf templates) |
| `VerificationService` | Email verification code generation, validation, expiry |
| `SupportService` | Support ticket persistence |

### repository/

Spring Data MongoDB interfaces. Services call repositories directly; nothing else does.

`AppNotificationRepository`, `BookingRepository`, `BookingTokenRepository`, `ConversationRepository`, `MessageRepository`, `ReportRepository`, `ServiceRepository`, `SupportEntryRepository`, `UserRepository`, `VerificationRepository`

### model/

MongoDB document models annotated with `@Document` and `@Data` (Lombok). Enums live alongside their parent model.

| Model | Key fields |
|---|---|
| `User` | `id`, `email`, `role` (STUDENT / PROVIDER / ADMIN), `stripeAccountId` |
| `Service` | `providerId`, `title`, `description`, `price`, `available` |
| `Booking` | `customerId`, `providerId`, `status` (`BookingStatus`), `stripePaymentIntentId` |
| `Conversation` | `participantIds`, list of embedded or referenced `Message` |
| `AppNotification` | `userId`, `type` (`NotificationType`), `read` |
| `Verification` | `userId`, `code`, `expiresAt` |

### dto/

Request and response shapes. All response DTOs use Lombok `@Value` (immutable); request DTOs use `@Data` and are validated with `@Valid` at the controller boundary.

**Auth/User:** `RegisterRequest`, `LoginRequest`, `AuthResponse`, `UpdateUserProfileRequest`, `ChangePasswordRequest`

**Services:** `CreateServiceRequest`, `UpdateServiceRequest`, `ServiceDto`

**Bookings:** `CreateBookingRequest`, `BookingResponse`, `ConfirmBookingRequest`, `SubmitReviewRequest`

**Payments:** `SetupIntentResult`, `PaymentIntentResult`, `ConnectOnboardingResponse`, `ConnectStatusResponse`

**Messaging:** `StartConversationRequest`, `ConversationResponse`, `SendMessageRequest`, `MessageResponse`, `NotificationResponse`, `UnreadCountResponse`

### exception/

All custom exceptions extend `RuntimeException`. `GlobalExceptionHandler` maps each to an appropriate HTTP status code. Notable groups:

- **Auth/Verification:** `InvalidVerificationCode`, `VerificationCodeExpired`
- **Booking:** `BookingStateException`, `UnauthorizedBookingRejectionException`, `BookingTokenException`
- **Payment:** `PaymentProcessingException`, `StripeConnectException`, `InvalidWebhookSignatureException`
- **User:** `EmailAlreadyExistsException`, `InvalidEmailDomainException`, `ResourceNotFoundException`
- **Chat:** `ConversationNotFoundException`, `UnauthorizedChatAccessException`

---

## Frontend

**Path:** `packages/frontend/`
**Stack:** React 18, TypeScript, Vite, React Router DOM 7

The frontend is a single-page application. It communicates with the backend over REST (Axios) and WebSocket (STOMP over SockJS) for real-time messaging and notifications.

### Directory Structure

```
src/
├── App.tsx                 # Root component — router and route definitions
├── main.tsx                # Vite entry point
├── components/             # Shared reusable UI components
├── pages/                  # Route-level page components
├── context/                # React context providers
├── utils/                  # API client, helpers, shared types
├── styles/                 # Global and page-level CSS
└── assets/                 # Static images and logos
```

### components/

Shared UI components used across multiple pages. Each component has a paired `.css` file. Examples:

| Component | Purpose |
|---|---|
| `NavBar` / `SideBar` / `Footer` | Application shell and navigation |
| `ServiceCard` / `ServiceDetailsModal` | Service listing display and detail view |
| `ServiceBooking` | Booking creation form and flow |
| `PaymentForm` | Stripe Elements payment form |
| `Modal` | Generic modal wrapper |
| `SearchBar` | Service search input |

### pages/

Route-level components, one per application screen. Each has a paired `.css` file.

| Page | Route purpose |
|---|---|
| `LandingPage` | Public marketing/entry page |
| `LoginPage` / `SignupPage` / `VerifyAccount` | Authentication flow |
| `HomePage` | Post-login dashboard |
| `ServiceDashboard` | Provider service management |
| `ProfilePage` / `ProviderProfilePage` | User and provider profile views |
| `Calendar` | Booking calendar for customers and providers |
| `Inbox` | Real-time messaging |
| `NotificationsPage` | In-app notification centre |
| `Settings` | Account settings |
| `AdminDashboard` | Admin user management |

### context/

| Context | Purpose |
|---|---|
| `WebSocketContext` | Manages the STOMP WebSocket connection; exposes subscription helpers to all pages that need live updates |

### utils/

| File | Purpose |
|---|---|
| `api.ts` | Axios instance with base URL and JWT auth header interceptor |
| `types.ts` | Shared TypeScript types (`ApiBookingStatus`, `NotificationType`, `ApiConversation`, etc.) |
| `helper.ts` | General-purpose helpers |
| `pricing.ts` | Pricing calculation utilities |
| `serviceFormatting.ts` | Service data formatting helpers |

---

## Key Cross-Cutting Flows

### Authentication
1. Client posts credentials to `POST /api/auth/login`
2. `AuthController` delegates to `UserService`, which validates via Spring Security's `AuthenticationManager`
3. `JwtService` issues a token; client stores it and attaches it as a `Bearer` header on every subsequent request
4. `JwtAuthenticationFilter` intercepts each request, parses the token, and populates the `SecurityContext`

### Payments (Stripe)
1. On booking creation, a Stripe customer and a `SetupIntent` are created for the customer
2. The frontend uses Stripe Elements to collect and confirm the payment method
3. On booking confirmation by the provider, `PaymentService` creates a `PaymentIntent` and charges the saved method
4. Stripe sends webhook events to `POST /api/payments/webhook`; `PaymentService` handles `payment_intent.succeeded` to mark the booking `CONFIRMED`
5. On cancellation or rejection, `PaymentService.refundPaymentIntent` issues a full refund and optionally reverses a connected-account transfer

### Real-time Messaging
1. On login, the frontend opens a STOMP WebSocket connection to `/ws`
2. `WebSocketAuthInterceptor` validates the JWT on the handshake
3. Messages sent via REST are persisted by `ConversationService` and pushed to the recipient's STOMP subscription
4. `NotificationService` pushes in-app notifications over the same connection
