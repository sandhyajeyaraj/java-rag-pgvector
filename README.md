# ComplianceIQ

An AI-powered policy Q&A system for fintech compliance. Upload regulatory documents, ask questions in natural language, and get structured answers with risk levels, key findings, and action items — backed by semantic search and LLM synthesis.

## What it does

- **Ingest** policy documents via PDF upload or direct text input
- **Ask** compliance questions (AML, KYC, GDPR, Sanctions, etc.)
- **Get** structured responses: summary, risk level, key findings, action items, source references
- **Audit** every query with provider used, confidence score, and latency

## Architecture

```
PDF / Text ──► Kafka ──► Chunking ──► Embeddings (nomic-embed-text) ──► pgvector
                                                                              │
Question ──► Vector Search ──► Top-K chunks ──► LLM ──────────────────────► Response
```

Two API tiers:
- **V1** — traditional CRUD for policy management
- **V2** — AI-driven ingestion and semantic Q&A

LLM providers fall back in order: **OpenAI → Anthropic → Ollama**, protected by a Resilience4j circuit breaker. Query results are cached in-memory with Caffeine (1-hour TTL).

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21, Spring Boot 3.3.5 |
| AI / LLMs | Spring AI 1.0.0, OpenAI (gpt-4o), Anthropic (claude-opus-4-7), Ollama (llama3.2) |
| Vector Store | PostgreSQL + pgvector (HNSW index, cosine distance, 768 dims) |
| Embeddings | nomic-embed-text via Ollama |
| Messaging | Apache Kafka |
| DB Migrations | Flyway |
| PDF Parsing | Apache PDFBox 3.0.3 |
| Resilience | Resilience4j circuit breaker, Caffeine cache |

## Prerequisites

- Java 21+
- PostgreSQL with the pgvector extension
- Ollama running locally with `nomic-embed-text` pulled
- Apache Kafka
- OpenAI and/or Anthropic API keys (optional — Ollama is the local fallback)

## Setup

**1. Configure API keys**

Set them as environment variables or update `src/main/resources/application.yml`:

```bash
export OPENAI_API_KEY=sk-...
export ANTHROPIC_API_KEY=sk-ant-...
```

**2. Create the database**

```sql
CREATE DATABASE complianceiq;
CREATE EXTENSION IF NOT EXISTS vector;
```

Flyway runs migrations automatically on startup.

**3. Pull the embedding model**

```bash
ollama pull nomic-embed-text
ollama pull llama3.2   # optional local LLM fallback
```

**4. Start Kafka**

```bash
docker run -d -p 9092:9092 apache/kafka
```

**5. Run**

```bash
./mvnw spring-boot:run
```

## API Reference

### V2 — AI Endpoints (`/api/v2/policies`)

**Upload a PDF**
```http
POST /api/v2/policies/upload
Content-Type: multipart/form-data

file=<pdf>  title=<optional>  category=<optional>
```

**Ingest text directly**
```http
POST /api/v2/policies/ingest
Content-Type: application/json

{
  "title": "AML Policy 2024",
  "content": "...",
  "category": "AML",
  "source": "compliance-team"
}
```

**Ask a question**
```http
POST /api/v2/policies/query
Content-Type: application/json

{
  "question": "What are the KYC requirements for high-risk customers?",
  "topK": 5,
  "category": "KYC"
}
```

Response includes: `summary`, `detailedExplanation`, `riskLevel` (LOW / MEDIUM / HIGH / CRITICAL), `keyFindings`, `actionItems`, `referencedPolicies`, `confidence`, `llmProvider`, `cachedResult`.

### V1 — Policy CRUD (`/api/v1/policies`)

```
GET    /api/v1/policies          # ?category= and ?keyword= supported
GET    /api/v1/policies/{id}
POST   /api/v1/policies
PUT    /api/v1/policies/{id}
DELETE /api/v1/policies/{id}
```

## Key Configuration (`application.yml`)

| Property | Default |
|---|---|
| `server.port` | `8080` |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/complianceiq` |
| `spring.ai.openai.chat.options.model` | `gpt-4o` |
| `spring.ai.ollama.embedding.options.model` | `nomic-embed-text` |
| `spring.ai.vectorstore.pgvector.dimensions` | `768` |
| `pdf.upload.directory` | `./uploadFile` |
| `spring.kafka.bootstrap-servers` | `localhost:9092` |
