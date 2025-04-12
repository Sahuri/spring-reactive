# 🔄 Real-Time Customer Data Streaming with Spring WebFlux

This project demonstrates how to build a real-time data streaming service using **Spring WebFlux** and **Server-Sent Events (SSE)**. It streams customer data from a reactive H2 database to clients whenever changes occur.

---

## 🚀 Features

- ✅ Reactive, non-blocking data stream using `Flux`
- ✅ Real-time push updates from the database to the frontend
- ✅ One-way server-to-client communication via Server-Sent Events (SSE)
- ✅ In-memory **H2 Database** with **R2DBC** for fully reactive backend
- ✅ Periodic polling for new/unsent customer records with automatic flagging

---

## 🧩 Tech Stack

- **Spring Boot** `WebFlux`
- **Project Reactor**
- **Server-Sent Events (SSE)`
- **R2DBC** (Reactive Relational Database Connectivity)
- **H2 Database** (In-memory for testing and development)

---

## 📡 How It Works

1. The backend polls the database every 2 seconds for new customer entries where `flag_sent = 0`.
2. New entries are emitted through a reactive `Flux` using a `Sinks.Many` publisher.
3. The `flag_sent` is immediately updated to prevent resending the same data.
4. Clients connected to `/stream/customers` receive real-time updates via SSE.

---

## 📂 Architecture

```text
+---------------------+       +-------------------------+       +---------------------+
|      H2 Database     | <---> |  NotificationListener   | <---> |  /stream/customers  |
| (new customer rows) |       |   (emits new data)      |       |  (SSE endpoint)     |
+---------------------+       +-------------------------+       +---------------------+
                                        |                              |
                                        v                              v
                               Reactively query DB           Client receives stream
                               every 2 seconds               of Customer JSON objects
```


## ▶️ How to Run the Application
✅ Prerequisites
- *Java 17 or higher*
- *Maven or Gradle*
- *Internet connection (to download dependencies)*

🧪 Run with Maven
```
./mvnw spring-boot:run

```
🧪 Run with Gradle
```
./gradlew bootRun

```
🧪 Or Build and Run the JAR
```
./mvnw clean package
java -jar target/your-app-name.jar
```
Replace your-app-name.jar with the actual JAR name created in the target folder.

🌐 Accessing the Stream
Once the application is running:
- Open your browser or an HTTP client like Postman, and access:
  ```
  http://localhost:8080/stream/customers

  ```
- You will see a continuous SSE (Server-Sent Events) stream of customer data updates.


