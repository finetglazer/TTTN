# Graduation Project - Microservices Architecture

A full-stack e-commerce application built with modern microservices architecture, featuring order management and payment processing capabilities.

## 🏗️ Architecture Overview

This project implements a microservices architecture with the following services:

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │  API Gateway    │    │   OrderService  │
│   (Next.js)     │◄──►│    Service      │◄──►│                 │
│                 │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │ PaymentService  │
                       │                 │
                       └─────────────────┘
```

## 🛠️ Tech Stack

### Frontend
- **Framework:** Next.js
- **Language:** TypeScript
- **Styling:** Tailwind CSS
- **UI Components:** Shadcn/ui
- **State Management:** TanStack Query
- **Form Handling:** React Hook Form + Zod

### Backend
- **Framework:** Java Spring Boot 3.4.x
- **Language:** Java 17
- **Database:** PostgreSQL/MongoDB (per service)
- **ORM:** TypeORM/Mongoose
- **Message Queue:** RabbitMQ/Kafka
- **Caching:** Redis
- **File Storage:** AWS S3
- **Build Tool:** Maven

### DevOps & Tools
- **Containerization:** Docker
- **CI/CD:** GitHub Actions
- **IDE Configuration:** IntelliJ IDEA
- **Version Control:** Git


## 🚀 Quick Start

### Prerequisites

- Java 17 or higher
- Node.js 18+ (for frontend)
- Maven 3.9+ (or use included wrapper)
- Docker (optional, for containerization)
- PostgreSQL/MongoDB (for databases)

## 🔧 Development

### Service Ports

| Service | Port | Description |
|---------|------|-------------|
| API Gateway | 8080 | Main entry point |
| Order Service | 8081 | Order management |
| Payment Service | 8082 | Payment processing |
| Frontend | 3000 | React/Next.js app |

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style

- Follow Java naming conventions
- Use meaningful variable and method names
- Add JavaDoc for public methods
- Write unit tests for new features

## 📅 Project Timeline

Refer to `Project schedule plan.xlsx` for detailed project milestones and deadlines.

## 🐛 Known Issues

- Database configuration needs to be completed
- Security implementation is pending
- Frontend integration is in progress

## 📞 Support

For questions and support, please contact:
- **Developer:** [Tran Manh Hung]
- **Email:** [tranhung174303@gmail.com]
- **Project Type:** Graduation Thesis Project

## 📝 License

This project is developed as part of a graduation thesis and is for educational purposes.

---

## 🎯 Learning Objectives

- Microservices architecture design and implementation
- Spring Boot framework mastery
- RESTful API development
- Database design and integration
- Modern frontend development with React/Next.js
- DevOps practices with Docker and CI/CD

---

*"The more I learn, the more I realize how much I don't know."* - Learning never stops! 🚀
