# Ask — Voice-to-AI Web App

A Spring Boot web application that lets users record or upload voice messages, transcribes them with OpenAI Whisper, classifies them as a **Question**, **Idea**, or **Other**, and returns an AI-generated response accordingly. All conversations are stored per user with JWT-based authentication.

---

## How It Works

```
Voice / Audio file
        │
        ▼
 OpenAI Whisper ──► Transcript
        │
        ▼
   GPT-4o classifies
        │
   ┌────┴──────────┐
QUESTION          IDEA         OTHER
   │                │              │
 Answer         Guideline    (no response)
        │
        ▼
  Saved to DB & shown to user
```

1. The user records audio in the browser or uploads an audio file.
2. The audio is sent to **OpenAI Whisper** and converted to text.
3. **GPT-4o** classifies the transcript and responds in a single call:
   - `QUESTION` → returns a clear, helpful answer
   - `IDEA` → returns practical guidelines / actionable steps
   - `OTHER` → returns an empty response
4. The transcript, type, and AI response are saved to the database (audio is never stored).
5. Users can browse their full conversation history with pagination.

---

## Features

- **In-browser voice recording** using the MediaRecorder API with a live timer
- **Audio file upload** (mp3, wav, m4a, ogg, webm, …)
- **Speech-to-text** via OpenAI Whisper
- **Smart classification + response** via GPT-4o in a single API call
- **Conversation history** with paginated list and detail views
- **JWT authentication** — register, login, and secure all endpoints
- **Thymeleaf UI** with markdown rendering for AI responses
- **Configurable limits** — max file size and max recording duration

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 24 |
| Framework | Spring Boot 4.0.5 |
| Security | Spring Security + JWT (jjwt 0.12.6) |
| Persistence | Spring Data JPA + PostgreSQL 16 |
| Templates | Thymeleaf |
| AI | OpenAI Java SDK 4.30.0 (Whisper + GPT-4o) |
| Build | Gradle |
| Infrastructure | Docker Compose |

---

## Getting Started

### Prerequisites

- Java 24+
- Docker & Docker Compose
- An [OpenAI API key](https://platform.openai.com/api-keys)

### 1. Start the database

```bash
docker-compose up -d
```

This starts a PostgreSQL 16 instance on port `5432` with database `ask`.

### 2. Set environment variables

```bash
export OPENAI_API_KEY=sk-...
```

Optional overrides (defaults shown):

```bash
export JWT_SECRET=<64-char-hex>       # custom JWT signing secret
export AUDIO_MAX_SIZE_MB=1            # max upload size in MB
export AUDIO_MAX_DURATION_SECONDS=60  # max recording length
```

### 3. Run the application

```bash
./gradlew bootRun
```

The app starts at **http://localhost:8080**.

---

## API Reference

All `/api/**` endpoints require the header:
```
Authorization: Bearer <token>
```

### Authentication

| Method | Path | Body | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | `{ "email", "password" }` | Create a new account |
| `POST` | `/api/auth/login` | `{ "email", "password" }` | Get a JWT token |

**Response:**
```json
{
  "token": "eyJhbGci..."
}
```

### Conversations

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/conversations` | Upload audio (`multipart/form-data`, field: `audio`) |
| `GET` | `/api/conversations` | List conversations (paginated: `?page=0&size=10`) |
| `GET` | `/api/conversations/{id}` | Get a single conversation |

**Conversation object:**
```json
{
  "id": 1,
  "transcript": "What is the capital of France?",
  "type": "QUESTION",
  "result": "The capital of France is Paris.",
  "createdAt": "2026-06-12T14:30:00"
}
```

---

## Project Structure

```
src/main/java/org/example/ask/
├── auth/               # JWT filter, auth controller, service, DTOs
├── config/             # Security config, OpenAI client config, exception handler
├── conversation/       # Conversation controller, service, DTOs
├── domain/             # JPA entities (User, Conversation), repositories, ConversationType enum
└── AskApplication.java

src/main/resources/
├── templates/          # Thymeleaf HTML pages (home, login, register, detail)
├── static/css/         # App stylesheet
└── application.yaml    # Configuration
```

---

## Configuration Reference

All settings are in `application.yaml` and can be overridden with environment variables:

| Environment Variable | Default | Description |
|---|---|---|
| `OPENAI_API_KEY` | *(required)* | OpenAI API key |
| `JWT_SECRET` | *(built-in dev secret)* | JWT signing secret (change in production) |
| `AUDIO_MAX_SIZE_MB` | `1` | Maximum audio file size in MB |
| `AUDIO_MAX_DURATION_SECONDS` | `60` | Maximum recording duration (enforced client-side) |

---

## Notes

- Audio files are **never persisted** — only the transcript and AI response are stored.
- The classification and response are done in a **single GPT-4o call** using JSON mode for efficiency.
- Each user can only access their own conversations (enforced server-side).