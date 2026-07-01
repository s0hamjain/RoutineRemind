# RoutineRemind M6 Deployment

This guide deploys the Spring Boot API and both web apps to Cloud Run, then
configures Cloud Scheduler to trigger FCM reminders.

Project: `routineremind`
Region: `us-central1`

Run these commands from the repository root:

```bash
cd /Users/sandeepjain/Downloads/RoutineRemind
```

## 1. One-time setup

```bash
gcloud config set project routineremind
gcloud services enable run.googleapis.com artifactregistry.googleapis.com cloudbuild.googleapis.com cloudscheduler.googleapis.com
```

Create an Artifact Registry Docker repository:

```bash
gcloud artifacts repositories create routineremind \
  --repository-format=docker \
  --location=us-central1 \
  --description="RoutineRemind containers"
```

## 2. Backend Cloud Run service

The backend should run as the `backend-sa` service account you already created.
It uses Application Default Credentials in Cloud Run, so do **not** deploy the
local JSON key.

Make sure `backend-sa` has these roles:

- Cloud Datastore User
- Storage Object Admin
- Cloud Speech Client
- Firebase Cloud Messaging API Admin

Set a scheduler shared secret:

```bash
export SCHEDULER_TOKEN="$(openssl rand -hex 32)"
```

Build and deploy:

```bash
gcloud builds submit backend \
  --tag us-central1-docker.pkg.dev/routineremind/routineremind/api:latest

gcloud run deploy routineremind-api \
  --image us-central1-docker.pkg.dev/routineremind/routineremind/api:latest \
  --region us-central1 \
  --service-account backend-sa@routineremind.iam.gserviceaccount.com \
  --allow-unauthenticated \
  --set-env-vars ROUTINEREMIND_GCP_PROJECT_ID=routineremind,ROUTINEREMIND_GCP_STORAGE_BUCKET=routineremind-media,ROUTINEREMIND_REMINDERS_SCHEDULER_TOKEN="$SCHEDULER_TOKEN"
```

Capture the backend URL:

```bash
export API_URL="$(gcloud run services describe routineremind-api --region us-central1 --format='value(status.url)')"
echo "$API_URL"
```

After deploying the web services, redeploy the backend with production CORS:

```bash
gcloud run services update routineremind-api \
  --region us-central1 \
  --set-env-vars ROUTINEREMIND_CORS_ALLOWED_ORIGINS="$ADMIN_URL,$PORTAL_URL"
```

## 3. Angular admin

```bash
gcloud builds submit web-admin \
  --config web-admin/cloudbuild.yaml \
  --substitutions _API_BASE_URL="$API_URL/api/v1"

gcloud run deploy routineremind-admin \
  --image us-central1-docker.pkg.dev/routineremind/routineremind/web-admin:latest \
  --region us-central1 \
  --allow-unauthenticated

export ADMIN_URL="$(gcloud run services describe routineremind-admin --region us-central1 --format='value(status.url)')"
echo "$ADMIN_URL"
```

## 4. Vue student portal

```bash
gcloud builds submit web-portal \
  --config web-portal/cloudbuild.yaml \
  --substitutions _API_BASE_URL="$API_URL/api/v1"

gcloud run deploy routineremind-portal \
  --image us-central1-docker.pkg.dev/routineremind/routineremind/web-portal:latest \
  --region us-central1 \
  --allow-unauthenticated

export PORTAL_URL="$(gcloud run services describe routineremind-portal --region us-central1 --format='value(status.url)')"
echo "$PORTAL_URL"
```

## 5. Cloud Scheduler reminders

The backend exposes:

```text
POST /api/v1/jobs/reminders/due
Header: X-Scheduler-Token: <SCHEDULER_TOKEN>
```

Create a Scheduler job that runs every morning:

```bash
gcloud scheduler jobs create http routineremind-morning-reminders \
  --location us-central1 \
  --schedule "0 8 * * *" \
  --time-zone "America/New_York" \
  --uri "$API_URL/api/v1/jobs/reminders/due" \
  --http-method POST \
  --headers "X-Scheduler-Token=$SCHEDULER_TOKEN"
```

Run it manually:

```bash
gcloud scheduler jobs run routineremind-morning-reminders --location us-central1
```

## 6. Device tokens

Clients register FCM tokens with:

```text
POST /api/v1/devices
Authorization: Bearer <Identity Platform ID token>
{ "token": "...", "platform": "web|android", "label": "optional" }
```

The reminder job scans today's schedules and sends one FCM notification to each
registered student device when that student has incomplete tasks.

Android registers its FCM token automatically after login. Web push requires a
VAPID key and service worker and is intentionally left as a follow-up so M6 can
ship with Android reminders first.

## 7. Storage CORS

For browser media uploads:

```bash
gsutil cors set gcp/storage-cors.json gs://routineremind-media
```
