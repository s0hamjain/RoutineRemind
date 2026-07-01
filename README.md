# RoutineRemind

RoutineRemind is a **patent-pending** mobile and web application, winner of the **Congressional App Challenge from Virginia's 10th District**, selected by U.S. Representative Jennifer Wexton. It is designed to aid children with autism and their families by giving parents a tool to build visual daily routines and giving children a natural-language chatbot to ask questions about their schedule.

---

## How it works

```
┌─────────────────────────────────────────────────────────────────┐
│                         Parent                                   │
│  Creates a visual routine: tasks, times, transition hints        │
│  Reviews child's questions and chatbot answers                   │
└──────────────────────────┬──────────────────────────────────────┘
                           │  REST API  (Spring Boot on Cloud Run)
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Backend                                   │
│  Java + Spring Boot                                              │
│  ├── Verifies identity (GCP Identity Platform)                   │
│  ├── Stores schedules & chat in Firestore                        │
│  ├── Sends FCM push reminders                                    │
│  └── Calls Vertex AI Gemini to answer child questions            │
└──────────────────────────┬──────────────────────────────────────┘
                           │
          ┌────────────────┴────────────────┐
          ▼                                 ▼
┌──────────────────┐             ┌──────────────────────┐
│  Angular Portal  │             │    Android App        │
│  (web browser)   │             │  Kotlin + Compose     │
│                  │             │  + C++ audio module   │
│  Parent mode:    │             │                       │
│  routine builder │             │  Visual schedule      │
│  chat history    │             │  Task completion      │
│                  │             │  Schedule chatbot     │
│  Child mode:     │             │  Voice input (C++     │
│  visual schedule │             │  pre-processing +     │
│  schedule chat   │             │  Speech-to-Text)      │
└──────────────────┘             └──────────────────────┘
```

---

## Tech stack

| Component | Technology |
|-----------|-----------|
| Android app | Kotlin + Jetpack Compose |
| Native audio module | C++ (NDK / JNI) — audio pre-processing for voice input |
| Backend API | Java + Spring Boot, hosted on Cloud Run |
| Schedule chatbot | Vertex AI Gemini — answers grounded in the child's schedule |
| Web portal | Angular — single app for both parent and child modes |
| Authentication | GCP Cloud Identity Platform — email/password and Google sign-in |
| Database | Cloud Firestore — schedules, users, chat history |
| Media storage | Cloud Storage — audio and video uploads |
| Speech-to-Text | Google Cloud Speech-to-Text — transcribes voice questions |
| Push notifications | Firebase Cloud Messaging — daily schedule reminders |

---

## Application components

### Parent experience

Parents sign up, choose the parent role, and link to their child's account using a short share code. From there they can build a daily routine for each linked child: adding tasks with a time, short description, visual icon, transition hint (e.g. "Then put on your shoes"), and a parent note the chatbot can draw from when answering questions. After a routine is active, parents can review every question their child asked and the answer the chatbot gave.

### Child experience

Children see their schedule as a visual card list — one card per task with a large icon, the time, and a calm description. They tap **Done** when they complete a task, which updates the schedule status in real time. At any point they can open the chat and ask questions in plain language: quick buttons for "What now?", "What next?", "When do I eat?", and "I need help", or they can type their own question. The chatbot answers using only the content of that day's routine.

### Schedule chatbot

The chatbot is a two-layer system. Common structural questions ("what do I do now?", "what comes next?") are answered instantly using deterministic logic that reads the current schedule state. All other questions — time lookups, detail questions, anything more specific — are passed to Vertex AI Gemini with the full schedule as context. Gemini is instructed to answer in 1–2 short, concrete sentences using simple words, and to say "I am not sure. Ask your parent!" if the answer is not in the schedule. Every question and answer is stored in Firestore so parents can review the chat history.

### Android native module (C++)

The Android app includes a C++ module built with the NDK via JNI. It provides audio pre-processing for voice-based questions: the raw audio buffer is processed in C++ before being uploaded to Cloud Storage and transcribed by Google Cloud Speech-to-Text. This keeps audio handling fast and off the main thread.

### Backend API

The Spring Boot backend is the single source of truth for all data operations. It verifies every request using a Firebase ID token, enforces role-based authorization (parents can create/edit schedules; children can complete tasks and use chat), manages parent–child linking via share codes, handles signed GCS upload URLs for media, calls Vertex AI Gemini for chat answers, sends FCM push reminders via Cloud Scheduler, and stores everything in Firestore through the Firebase Admin SDK. It runs as a stateless container on Cloud Run.

---

## Repository layout

```
android/      Kotlin app + C++ NDK audio module
backend/      Java + Spring Boot API
web-admin/    Angular parent/child web portal
gcp/          Firestore rules, indexes, and deployment config
docs/         Architecture specification
secrets/      Service-account credentials (git-ignored)
```
