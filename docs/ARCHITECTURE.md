# RoutineRemind — Architecture & Spec (v1)

A schedule/chat app for children with autism, managed by parents. Parents build
visual routines; children ask questions about the routine and receive simple,
schedule-grounded answers. Rebuilt on a Google Cloud–native stack.

## Tech stack

| Layer | Technology | Hosting / Service |
|-------|-----------|-------------------|
| Android app | Kotlin (+ Java interop) | Android (Android-only) |
| Native audio/video/speech | C++ (NDK / JNI) | Bundled in Android app |
| Backend API (primary) | Java + Spring Boot | Cloud Run |
| Auth | Cloud Identity Platform | GCP-managed; backend verifies tokens |
| Database | Cloud Firestore (Native mode) | via Firebase Admin SDK |
| File storage | Cloud Storage (GCS) | bucket `routineremind-media` |
| Speech | Cloud Speech-to-Text | pairs with C++ pre-processing |
| Push | FCM messaging API | server-side from backend |
| Web portal | Angular | Cloud Run |
| Schedule chatbot | Vertex AI Gemini | Called by Spring Boot backend |

GCP project: `routineremind`.

## Repository layout

```
android/       Kotlin app + native-media/ C++ NDK module
backend/       Spring Boot API (primary)
web-admin/     Angular — single parent/child portal
gcp/           Firestore rules + composite indexes
secrets/       Service-account key + client config (git-ignored)
docs/          This spec
```

## Roles

- **Parent** — creates and manages visual schedules for linked children.
- **Student** — views today's/previous schedules, completes tasks, and asks schedule questions.
- Parents link to students via a short **share code**.

## Auth flow

1. Client signs in with Identity Platform (email/password or Google) → ID token.
2. Client calls `POST /api/v1/auth/session` with `Authorization: Bearer <token>`.
3. Backend verifies the token (Firebase Admin SDK) and upserts `users/{uid}`.
4. If `role` unset → client shows role-select → `POST /api/v1/me/role`.
5. All subsequent requests carry the token; backend authorizes by role + links.

## Firestore schema

```
users/{uid}
  displayName, email, role ("student"|"parent"),
  linkedUserIds[], shareCode (students), photoUrl, createdAt, updatedAt

schedules/{scheduleId}
  ownerUid, date ("YYYY-MM-DD"), title,
  status ("upcoming"|"active"|"completed"),
  items[]: { id, time ("HH:mm"), title, description, icon, imageUrl,
             parentNote, audioUrl, transitionHint, sortOrder, completed, completedAt },
  createdAt, updatedAt

questions/{questionId}
  scheduleId, ownerUid, prompt, type ("text"|"audio"|"video"), order, createdAt

responses/{responseId}
  questionId, studentUid, text, mediaUrl, transcript, createdAt

chatMessages/{messageId}
  ownerUid, question, answer, scheduleId, matchedItemId, source, createdAt
```

Composite indexes: `schedules(ownerUid ASC, date DESC)`,
`questions(scheduleId ASC, order ASC)`, `responses(questionId ASC, createdAt DESC)`.

Security rules: restrictive — clients never read/write Firestore directly;
all access flows through the backend service account.

## API contract (`/api/v1`)

Auth & profile:
```
POST /auth/session      verify token, create/return profile on first login
GET  /me                current user profile + role
PATCH /me               update displayName, photoUrl
POST /me/role           set role: "student" | "parent"
POST /me/link           parent links a student by share code
```

Schedules:
```
GET  /schedule/today                 today's schedule (student, or linked student for parent)
GET  /schedules?before=DATE&limit=20 previous schedules (paginated)
GET  /schedules/{id}                 single schedule detail
POST /schedules                      create (parent)
PATCH /schedules/{id}                edit title/items (parent)
POST /schedules/{id}/items/{itemId}/complete   mark task done (student)
```

Questions & responses:
```
GET  /schedules/{id}/questions
POST /questions/{id}/responses         submit text response
POST /questions/{id}/responses/media   get signed GCS upload URL
POST /responses/{id}/transcribe        trigger Speech-to-Text
```

Schedule chat:
```
POST /chat                 child asks a schedule-grounded question
GET  /chat/history         parent reviews recent child questions
```

Devices / push:
```
POST /devices    register FCM token for current user
```

Error shape:
```json
{ "error": { "code": "UNAUTHORIZED", "message": "..." } }
```

## Design system

- **Font:** Inter.
- **Palette:** primary `#4F46E5`, primary-dark `#3730A3`, accent `#06B6D4`,
  success `#10B981`, warning `#F59E0B`, danger `#EF4444`,
  background `#F8FAFC`, surface `#FFFFFF`,
  text-primary `#0F172A`, text-secondary `#64748B`, border `#E2E8F0`.
- **Spacing (8pt):** 4, 8, 12, 16, 24, 32, 48.
- **Radius:** sm 8, md 12, lg 16, pill 999.
- **Type scale:** Display 32/40, H1 24/32, H2 20/28, Body 16/24, Caption 13/18, Label 12/16.
- Light mode only for v1 (tokens structured to add dark mode later).

## Milestones

1. **M1** Scaffold + auth slice: login on all clients, `POST /auth/session`, `GET /schedule/today`.
2. **M2** Schedules: today + previous + detail, task completion.
3. **M3** Role select + parent↔student linking (share code).
4. **M4** Schedule-aware chat: child questions + Gemini grounded answers.
5. **M5** Audio/video (C++ module) + Speech-to-Text for voice questions.
6. **M6** FCM reminders + polish + Cloud Run deploy.
