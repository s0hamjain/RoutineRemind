# RoutineRemind

RoutineRemind is a ***patent-pending*** mobile and web application that was selected by U.S. Representative Jennifer Wexton as the **winner of the Congressional App Challenge from Virginia's 10th District**. The application is designed to **aid individuals with cognitive and speech impairments**, with a focus on children with autism and their families. Parents create daily schedules for their children by recording action items for the day. When a child asks a question about their routine, the app plays back the matching recording from the parent, which **reduces maladaptive behavior in children with autism**. The app incorporates **machine learning** and **natural language processing** to match the child's question with the corresponding recording.

> **Note:** The project is being rebuilt on a Google Cloud–native stack. See
> [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the full spec.

## Tech stack

- **Android** — Kotlin + Jetpack Compose
- **Native module** — C++ (NDK/JNI) for audio/video + speech pre-processing
- **Backend** — Java + Spring Boot (primary API), on Cloud Run
- **Web admin** — Angular (parent/admin dashboard)
- **Web portal** — Vue (student portal)
- **Google Cloud** — Cloud Identity Platform (auth), Firestore (data), Cloud
  Storage (media), Cloud Speech-to-Text, FCM (push)

## Monorepo layout

```
android/       Kotlin app + C++ NDK module        (see android/README.md)
backend/       Spring Boot API                    (primary)
web-admin/     Angular — parent/admin dashboard
web-portal/    Vue — student portal
gcp/           Firestore rules + indexes          (see gcp/README.md)
secrets/       Service-account key + config       (git-ignored)
docs/          Architecture & spec
```

## Dev quickstart

```bash
# 1) Backend (http://localhost:8080)
cd backend && ./mvnw spring-boot:run

# 2) Angular admin (http://localhost:4200)
cd web-admin && npm start

# 3) Vue portal (http://localhost:5173)
cd web-portal && npm run dev

# 4) Android — open android/ in Android Studio (see android/README.md)
```

The backend reads its service-account key from `secrets/` (see
`backend/src/main/resources/application.properties`). The web clients use the
Identity Platform web config baked into their environment files.

## Legacy

The previous React Native / Expo implementation still lives at the repository
root (`App.tsx`, `screens/`, `components/`, `functions/`, etc.) and is being
superseded by the stack above. It can be removed once migration is complete.
