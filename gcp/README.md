# GCP configuration

Firestore security rules and composite indexes for RoutineRemind.

## Deploy rules & indexes

Requires the Firebase CLI (`npm i -g firebase-tools`) and `firebase login`.

```bash
cd gcp
firebase use routineremind
firebase deploy --only firestore:rules,firestore:indexes
```

The rules are **restrictive** (deny all direct client access) — every read/write
goes through the backend service account, which bypasses rules.

## Configure Storage CORS for media uploads

Browser clients upload media directly to GCS signed URLs, so the bucket needs
CORS for local dev:

```bash
cd gcp
gsutil cors set storage-cors.json gs://routineremind-media
```

Run this once after creating the `routineremind-media` bucket.

## Seed test data

Use the seed script to create a sample "today" schedule without manual Firestore
entry.

```bash
cd gcp
npm install
npm run seed -- <student-uid>
```

Find the student's `uid` in Identity Platform → Users, or in Firestore
`users/{uid}` after the student signs in once.

For a quick smoke test that does not match a real login user:

```bash
cd gcp
npm install
npm run seed
```

That creates `users/demo-student` and `schedules/demo-student-<today>`.
