# Student Report Service

A standalone Java (Spring Boot) microservice that generates a downloadable PDF
report for a student. It does not access the database directly; it consumes the
Node.js backend API to fetch student data and renders the PDF from the response.

## Endpoint

```
GET /api/v1/students/{id}/report
```

Returns a `application/pdf` document for the given student id.

## How it works

1. Authenticates against the Node.js backend (`POST /api/v1/auth/login`) and
   keeps the returned session cookies (`accessToken`, `refreshToken`,
   `csrfToken`).
2. Fetches the student from `GET /api/v1/students/{id}`, sending the cookies and
   the `x-csrf-token` header required by the backend.
3. Builds a PDF report from the JSON response and streams it back to the caller.

## Prerequisites

- Java 21+
- Maven 3.9+
- The PostgreSQL database and the Node.js backend must be running and seeded.

## Configuration

Configured via `src/main/resources/application.properties` or environment
variables:

| Property            | Env var             | Default                          |
| ------------------- | ------------------- | -------------------------------- |
| `server.port`       | `SERVER_PORT`       | `5008`                           |
| `backend.base-url`  | `BACKEND_BASE_URL`  | `http://localhost:5007/api/v1`   |
| `backend.username`  | `BACKEND_USERNAME`  | `admin@school-admin.com`         |
| `backend.password`  | `BACKEND_PASSWORD`  | `3OU4zn3q6Zh9`                   |

## Run

```bash
cd Java-service
mvn spring-boot:run
```

## Test

With the database, backend and this service running:

```bash
curl -o student-1-report.pdf http://localhost:5008/api/v1/students/1/report
```

The response is a PDF file containing the student's personal, academic,
guardian and address details.
