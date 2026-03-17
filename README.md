# Enterprise Expense & Compliance Auditing System

## Overview
This is an enterprise-grade financial governance system with Role-Based Access Control, Expense Submission, Compliance Rule Engine, Multi-Level Approval Workflows, and Audit Logging.

## Tech Stack
- **Backend**: Spring Boot 3.2, Spring Security (JWT), Spring Data JPA, MySQL (H2 fallback possible if configured)
- **Frontend**: React 19, Vite, Tailwind CSS, Axios, Recharts
- **Database**: MySQL 8.0

## Prerequisites
- Java 17+
- Node.js 18+
- MySQL Server

## Setup Instructions

### 1. Database Setup
Create a MySQL database named `expense_audit_db`.
```sql
CREATE DATABASE expense_audit_db;
```
Update `backend/src/main/resources/application.properties` with your MySQL credentials if different from `root/password`.

### 2. Backend Setup
Navigate to the `backend` directory:
```bash
cd backend
mvn clean install
mvn spring-boot:run
```
The backend will start on `http://localhost:8080`.
**Note**: On first run, the application will seed default roles (EMPLOYEE, MANAGER, FINANCE, COMPLIANCE, ADMIN).

### 3. Frontend Setup
Navigate to the `frontend` directory:
```bash
cd frontend
npm install
npm run dev
```
The frontend will start on `http://localhost:5173`.

## Default Roles & Permissions
- **Employee**: Submit expenses, view own history.
- **Manager**: Approve/Reject expenses (First Level).
- **Finance**: Approve/Reject expenses (Second Level), Final processing.
- **Admin**: Manage Users, Rules, Budgets.

## API Documentation
Once backend is running, you can test endpoints using Postman or curl.
- Auth: `/api/auth/signin`, `/api/auth/signup`
- Expenses: `/api/expenses`
- Approvals: `/api/approvals`
- Admin: `/api/admin`

## Folder Structure
- `backend`: Spring Boot application.
- `frontend`: React Vite application.
