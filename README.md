# Search Engine – Stage 2
**Big Data – Bachelor’s Degree in Data Science and Engineering**  
**University of Las Palmas de Gran Canaria**

## Project Description

This project implements a Service-Oriented Architecture (SOA) that extends the data layer from Stage 1 into a fully distributed and scalable system.  
The goal is to develop a distributed search engine composed of independent microservices that communicate through REST APIs using JSON as the data exchange format.

Each service performs a specific role within the architecture:
- **Ingestion Service:** Downloads books from Project Gutenberg and stores them in the datalake.
- **Indexing Service:** Processes text files, extracts metadata, and builds inverted indexes stored in the datamarts.
- **Search Service:** Exposes endpoints for keyword search, filtering, and ranking.
- **Control Service:** Coordinates the workflow between services, ensuring consistency and execution order.

---

## System Architecture
```text
+------------------+       +------------------+       +------------------+
|  Ingestion       | --->  |  Indexing        | --->  |  Search          |
|  Service         |       |  Service         |       |  Service         |
+------------------+       +------------------+       +------------------+
         ^                                                    |
         |                                                    v
         +-------------------- Control Service ----------------+
```
Each service is an independent **Maven** module, capable of running as a standalone process.  
Communication between services is performed via HTTP, and the Control Service orchestrates their interactions.

---

## Repository Structure
```text
STAGE_2/
│
├── benchmarks/                 # JMH benchmarks and performance results
│   ├── results/
│   ├── src/
│   └── plot_benchmark_results.py
│
├── control-service/            # Orchestration service
│   ├── src/
│   ├── target/
│   └── pom.xml
│
├── ingestion-service/          # Data ingestion service
│   ├── src/
│   ├── target/
│   └── pom.xml
│
├── indexing-service/           # Book indexing service
│   ├── src/main/java/org/ulpgc/bd/indexing/
│   │   ├── api/
│   │   ├── model/
│   │   ├── service/
│   │   └── util/
│   └── pom.xml
│
├── search-service/             # Search and filtering service
│   ├── src/
│   ├── target/
│   └── pom.xml
│
└── README.md                   # This document
```

---

## Technologies Used

| Technology | Purpose |
|-------------|----------|
| **Java 17** | Core programming language |
| **Maven** | Dependency management and build automation |
| **Javalin 6.1.3** | Lightweight REST framework |
| **Gson 2.11.0** | JSON serialization and deserialization |
| **SLF4J Simple 2.0.9** | Logging system |
| **JMH** | Java Microbenchmark Harness for performance evaluation |

---

## How to Build and Run

1. **Build each microservice:**
   ```bash
   mvn clean package

   # Ingestion Service:
   cd ingestion-service
   mvn clean package -DskipTests

   # Indexing Service
   cd indexing-service
   mvn clean package -DskipTests

   # Search Service
   cd search-service
   mvn clean package -DskipTests

   # Control Service
   cd control-service
   mvn clean package -DskipTests

2. **Run Each Service (from Its Own Folder):**
   ```bash
   # Ingestion Service:
   cd ingestion-service
   mvn exec:java "-Dexec.mainClass=org.ulpgc.bd.ingestion.IngestionServiceApp"

   # Indexing Service
   cd indexing-service
   mvn exec:java "-Dexec.mainClass=org.ulpgc.bd.indexing.IndexingServiceApp"

   # Search Service
   cd search-service
   mvn exec:java "-Dexec.mainClass=org.ulpgc.bd.search.SearchServiceApp"

   # Control Service
   cd control-service
   mvn exec:java "-Dexec.mainClass=org.ulpgc.bd.control.ControlServiceApp"


| Service  | Port | Health Endpoint              |
|-----------|------|------------------------------|
| Ingestion | 7001 | `/ingest/status/{book_id}`   |
| Indexing  | 7002 | `/index/status`              |
| Search    | 7003 | `/search?q={term}`           |
| Control   | 7000 | `/control/run`               |

## REST API Overview

### Ingestion Service
- `POST /ingest/{book_id}` – Downloads and stores a book in the datalake.  
- `GET /ingest/status/{book_id}` – Checks download status.  
- `GET /ingest/list` – Lists all downloaded books.

### Indexing Service
- `POST /index/update/{book_id}` – Indexes a single book.  
- `POST /index/rebuild` – Rebuilds the entire index.  
- `GET /index/status` – Returns indexing statistics.

### Search Service
- `GET /search?q={term}` – Searches for a keyword.  
- `GET /search?q={term}&author={name}` – Filters by author.  
- `GET /search?q={term}&language={code}` – Filters by language.  
- `GET /search?q={term}&year={YYYY}` – Filters by year.

### Control Service
Coordinates the full workflow:
1. Calls the **Ingestion Service**.  
2. Waits for confirmation and calls the **Indexing Service**.  
3. Notifies the **Search Service** to refresh data.

## Benchmarking

The **Java Microbenchmark Harness (JMH)** framework is used to measure:
- Text tokenization and normalization performance  
- Metadata extraction and index update efficiency  
- Query latency and ranking operations  
- Index construction and lookup speed  

### Example Execution

1. **Build the benchmark JAR:**
   ```bash
   mvn clean package
2. **Run the benchmarks:**
  java -jar target/benchmarks.jar -rf csv
### Output and Results

Benchmark results are automatically saved in the directory:
  /benchmarks/results/

---

## Authors

* Leonoor Antje Barton
* Adrian Budzich
* Martyna Chmielińska 
* Angela López Dorta
* Pablo Mendoza Rodriguez

```

