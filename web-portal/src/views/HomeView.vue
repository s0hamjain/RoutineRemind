<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { api, type AppUser, type Question, type Schedule } from '../services/api';
import { authService } from '../services/auth';

const router = useRouter();
const user = ref<AppUser | null>(null);
const schedule = ref<Schedule | null>(null);
const questions = ref<Question[]>([]);
const loading = ref(true);
const error = ref('');
const completingItem = ref('');
const responseText = ref<Record<string, string>>({});
const answeringQuestion = ref('');
const selectedMedia = ref<Record<string, File | null>>({});
const uploadingQuestion = ref('');
const transcriptByQuestion = ref<Record<string, string>>({});

const todayLabel = new Date().toLocaleDateString(undefined, {
  weekday: 'long',
  month: 'long',
  day: 'numeric',
});

async function loadToday() {
  try {
    schedule.value = await api.todaySchedule();
    questions.value = await api.questions(schedule.value.id);
  } catch {
    schedule.value = null; // 404 → empty state
    questions.value = [];
  }
}

async function bootstrap() {
  loading.value = true;
  error.value = '';
  try {
    user.value = await api.createSession();
    if (user.value.role) {
      await loadToday();
    }
  } catch (e: any) {
    error.value = e?.message ?? 'Failed to load your account.';
  } finally {
    loading.value = false;
  }
}

async function chooseRole(role: 'student' | 'parent') {
  loading.value = true;
  try {
    user.value = await api.setRole(role);
    await loadToday();
  } catch (e: any) {
    error.value = e?.message ?? 'Failed to set role.';
  } finally {
    loading.value = false;
  }
}

async function completeItem(itemId: string) {
  if (!schedule.value) return;
  completingItem.value = itemId;
  try {
    schedule.value = await api.completeItem(schedule.value.id, itemId);
  } catch (e: any) {
    error.value = e?.message ?? 'Could not complete item.';
  } finally {
    completingItem.value = '';
  }
}

async function submitAnswer(questionId: string) {
  const text = responseText.value[questionId]?.trim();
  if (!text) {
    error.value = 'Answer cannot be empty.';
    return;
  }
  answeringQuestion.value = questionId;
  error.value = '';
  try {
    await api.submitTextResponse(questionId, text);
    responseText.value = { ...responseText.value, [questionId]: '' };
  } catch (e: any) {
    error.value = e?.message ?? 'Could not submit answer.';
  } finally {
    answeringQuestion.value = '';
  }
}

function handleMediaSelected(questionId: string, event: Event) {
  const input = event.target as HTMLInputElement;
  selectedMedia.value = {
    ...selectedMedia.value,
    [questionId]: input.files?.[0] ?? null,
  };
}

async function submitMedia(questionId: string) {
  const file = selectedMedia.value[questionId];
  if (!file) {
    error.value = 'Choose an audio or video file first.';
    return;
  }
  uploadingQuestion.value = questionId;
  error.value = '';
  try {
    const upload = await api.createMediaUpload(questionId, file.type || 'audio/webm');
    const put = await fetch(upload.uploadUrl, {
      method: 'PUT',
      headers: { 'Content-Type': upload.contentType },
      body: file,
    });
    if (!put.ok) {
      throw new Error(`Upload failed (${put.status})`);
    }
    const response = await api.transcribeResponse(upload.responseId);
    transcriptByQuestion.value = {
      ...transcriptByQuestion.value,
      [questionId]: response.transcript || '(No speech detected)',
    };
    selectedMedia.value = { ...selectedMedia.value, [questionId]: null };
  } catch (e: any) {
    error.value = e?.message ?? 'Could not upload media.';
  } finally {
    uploadingQuestion.value = '';
  }
}

async function signOut() {
  await authService.signOut();
  await router.push('/login');
}

onMounted(bootstrap);
</script>

<template>
  <header class="topbar">
    <div class="brand">
      <div class="brand-mark">RR</div>
      <strong>RoutineRemind</strong>
    </div>
    <div class="user">
      <span class="rr-caption" v-if="user">{{ user.email }}</span>
      <button class="rr-btn rr-btn-ghost" @click="signOut">Sign out</button>
    </div>
  </header>

  <main class="content">
    <div class="rr-card" v-if="loading">
      <span class="rr-caption">Loading…</span>
    </div>

    <div class="rr-card" v-else-if="error">
      <div class="rr-error">{{ error }}</div>
    </div>

    <!-- Role selection -->
    <div class="rr-card role-card" v-else-if="!user?.role">
      <h1>Welcome to RoutineRemind</h1>
      <p class="rr-caption">Tell us who you are to get started.</p>
      <div class="role-grid">
        <button class="role-option" @click="chooseRole('student')">
          <span class="emoji">🎓</span>
          <strong>I'm a Student</strong>
          <span class="rr-caption">View my schedule and complete tasks.</span>
        </button>
        <button class="role-option" @click="chooseRole('parent')">
          <span class="emoji">👨‍👩‍👧</span>
          <strong>I'm a Parent</strong>
          <span class="rr-caption">Manage schedules from the admin dashboard.</span>
        </button>
      </div>
    </div>

    <div class="rr-card parent-card" v-else-if="user?.role === 'parent'">
      <span class="emoji">👨‍👩‍👧</span>
      <h1>Use the parent dashboard</h1>
      <p class="rr-caption">
        This portal is optimized for students. Parents can link students, choose between linked students,
        and create schedules from the Angular admin dashboard.
      </p>
      <a class="rr-btn rr-btn-primary dashboard-link" href="http://localhost:4200">Open parent dashboard</a>
    </div>

    <template v-else>
      <!-- Student share code -->
      <div class="rr-card" v-if="user?.role === 'student'">
        <span class="rr-label">Your share code</span>
        <div class="share-code">{{ user?.shareCode }}</div>
        <p class="rr-caption">Give this to your parent so they can link to your account.</p>
      </div>

      <section class="today">
        <div class="today-head">
          <h1>Today's schedule</h1>
          <span class="rr-caption">{{ todayLabel }}</span>
        </div>

        <div class="rr-card" v-if="schedule">
          <div class="sched-title">
            <h2>{{ schedule.title }}</h2>
            <span class="rr-badge">{{ schedule.status }}</span>
          </div>
          <ul class="items">
            <li class="item" v-for="item in schedule.items" :key="item.id" :class="{ done: item.completed }">
              <span class="time">{{ item.time }}</span>
              <div class="item-body">
                <strong>{{ item.title }}</strong>
                <span class="rr-caption" v-if="item.description">{{ item.description }}</span>
              </div>
              <button
                v-if="user?.role === 'student' && !item.completed"
                class="complete-btn"
                :disabled="completingItem === item.id"
                @click="completeItem(item.id)"
              >
                {{ completingItem === item.id ? 'Saving…' : 'Mark done' }}
              </button>
              <span v-else class="status-dot" :class="{ done: item.completed }"></span>
            </li>
          </ul>
        </div>

        <div class="rr-card empty" v-else>
          <span class="emoji">📅</span>
          <strong>No schedule for today</strong>
          <p class="rr-caption">Your schedule will appear here once your parent adds one.</p>
        </div>
      </section>

      <section class="questions" v-if="schedule">
        <div class="today-head">
          <h1>Questions</h1>
          <span class="rr-caption">{{ questions.length }} prompts</span>
        </div>

        <div class="rr-card question-card" v-if="questions.length">
          <div class="question" v-for="question in questions" :key="question.id">
            <strong>{{ question.prompt }}</strong>
            <textarea
              class="rr-input answer"
              v-model="responseText[question.id]"
              rows="3"
              placeholder="Type your answer..."
            ></textarea>
            <button
              class="rr-btn rr-btn-primary"
              :disabled="answeringQuestion === question.id"
              @click="submitAnswer(question.id)"
            >
              {{ answeringQuestion === question.id ? 'Submitting…' : 'Submit answer' }}
            </button>

            <div class="media-answer">
              <span class="rr-label">Audio / video answer</span>
              <input
                class="rr-input"
                type="file"
                accept="audio/*,video/*"
                @change="handleMediaSelected(question.id, $event)"
              />
              <button
                class="rr-btn rr-btn-ghost"
                :disabled="uploadingQuestion === question.id"
                @click="submitMedia(question.id)"
              >
                {{ uploadingQuestion === question.id ? 'Uploading + transcribing…' : 'Upload and transcribe' }}
              </button>
              <p class="rr-caption" v-if="transcriptByQuestion[question.id]">
                Transcript: {{ transcriptByQuestion[question.id] }}
              </p>
            </div>
          </div>
        </div>

        <div class="rr-card empty" v-else>
          <strong>No questions yet</strong>
          <p class="rr-caption">Questions from your parent will appear here.</p>
        </div>
      </section>
    </template>
  </main>
</template>

<style scoped>
.topbar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 24px; background: var(--rr-surface); border-bottom: 1px solid var(--rr-border);
}
.brand { display: flex; align-items: center; gap: 10px; }
.brand-mark {
  width: 32px; height: 32px; border-radius: 8px; background: var(--rr-primary);
  color: #fff; display: grid; place-items: center; font-weight: 700; font-size: 13px;
}
.user { display: flex; align-items: center; gap: 12px; }
.content { max-width: 640px; margin: 0 auto; padding: 32px 24px; display: flex; flex-direction: column; gap: 24px; }
.role-card h1 { margin-bottom: 4px; }
.role-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 20px; }
.role-option {
  display: flex; flex-direction: column; gap: 6px; text-align: left; padding: 20px;
  border: 1px solid var(--rr-border); border-radius: var(--rr-radius-md); background: #fff;
  cursor: pointer; transition: border-color 0.15s, box-shadow 0.15s;
}
.role-option:hover { border-color: var(--rr-primary); box-shadow: var(--rr-shadow-md); }
.emoji { font-size: 28px; }
.share-code { font-size: 28px; font-weight: 700; letter-spacing: 0.15em; color: var(--rr-primary); margin: 8px 0; }
.today-head { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 12px; }
.sched-title { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.items { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; }
.item { display: flex; align-items: center; gap: 16px; padding: 14px; border: 1px solid var(--rr-border); border-radius: var(--rr-radius-md); }
.item.done { opacity: 0.6; }
.item.done strong { text-decoration: line-through; }
.time { font-weight: 600; color: var(--rr-primary); min-width: 56px; }
.item-body { display: flex; flex-direction: column; flex: 1; }
.status-dot { width: 12px; height: 12px; border-radius: 50%; border: 2px solid var(--rr-border); }
.status-dot.done { background: var(--rr-success); border-color: var(--rr-success); }
.complete-btn {
  border: 1px solid var(--rr-border);
  border-radius: var(--rr-radius-pill);
  background: #fff;
  color: var(--rr-primary);
  font: inherit;
  font-size: 12px;
  font-weight: 600;
  padding: 6px 10px;
  cursor: pointer;
}
.complete-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.empty { text-align: center; display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 40px; }
.parent-card { display: flex; flex-direction: column; align-items: flex-start; gap: 10px; }
.dashboard-link { display: inline-flex; text-decoration: none; margin-top: 8px; }
.questions { display: flex; flex-direction: column; gap: 12px; }
.question-card { display: flex; flex-direction: column; gap: 18px; }
.question { display: flex; flex-direction: column; gap: 10px; }
.answer { resize: vertical; min-height: 86px; }
.media-answer {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 10px;
  border-top: 1px solid var(--rr-border);
}
</style>
