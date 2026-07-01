<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { authService } from '../services/auth';

type Mode = 'signin' | 'signup' | 'reset';

const router = useRouter();
const email = ref('');
const password = ref('');
const mode = ref<Mode>('signin');
const busy = ref(false);
const error = ref('');
const info = ref('');

function setMode(m: Mode) {
  mode.value = m;
  error.value = '';
  info.value = '';
}

function primaryLabel() {
  return mode.value === 'signup' ? 'Create account' : mode.value === 'reset' ? 'Send reset link' : 'Sign in';
}

function friendly(code: string) {
  const map: Record<string, string> = {
    'auth/invalid-credential': 'Incorrect email or password.',
    'auth/user-not-found': 'No account found for that email.',
    'auth/wrong-password': 'Incorrect password.',
    'auth/email-already-in-use': 'An account already exists for that email.',
    'auth/weak-password': 'Password should be at least 6 characters.',
    'auth/invalid-email': 'Please enter a valid email address.',
  };
  return map[code] ?? 'Something went wrong. Please try again.';
}

async function submit() {
  error.value = '';
  info.value = '';
  busy.value = true;
  try {
    if (mode.value === 'signin') {
      await authService.signIn(email.value, password.value);
      await router.push('/');
    } else if (mode.value === 'signup') {
      await authService.signUp(email.value, password.value);
      await router.push('/');
    } else {
      await authService.resetPassword(email.value);
      info.value = 'Password reset email sent. Check your inbox.';
    }
  } catch (e: any) {
    error.value = friendly(e?.code ?? e?.message);
  } finally {
    busy.value = false;
  }
}

async function google() {
  error.value = '';
  busy.value = true;
  try {
    await authService.signInWithGoogle();
    await router.push('/');
  } catch (e: any) {
    error.value = friendly(e?.code ?? e?.message);
  } finally {
    busy.value = false;
  }
}
</script>

<template>
  <div class="auth-wrap">
    <div class="rr-card auth-card">
      <div class="brand">
        <div class="brand-mark">RR</div>
        <div>
          <h1>RoutineRemind</h1>
          <p class="rr-caption">Student portal</p>
        </div>
      </div>

      <h2 class="auth-title">
        {{ mode === 'signin' ? 'Welcome back' : mode === 'signup' ? 'Create your account' : 'Reset password' }}
      </h2>

      <div class="rr-field">
        <label class="rr-label" for="email">Email</label>
        <input id="email" class="rr-input" type="email" v-model="email" placeholder="you@example.com" />
      </div>

      <div class="rr-field" v-if="mode !== 'reset'">
        <label class="rr-label" for="password">Password</label>
        <input id="password" class="rr-input" type="password" v-model="password" placeholder="••••••••" />
      </div>

      <div class="rr-error" v-if="error">{{ error }}</div>
      <div class="info" v-if="info">{{ info }}</div>

      <button class="rr-btn rr-btn-primary rr-btn-block" :disabled="busy" @click="submit">
        {{ busy ? 'Please wait…' : primaryLabel() }}
      </button>

      <button v-if="mode !== 'reset'" class="rr-btn rr-btn-ghost rr-btn-block google" :disabled="busy" @click="google">
        Continue with Google
      </button>

      <div class="switches">
        <template v-if="mode === 'signin'">
          <button class="link" @click="setMode('reset')">Forgot password?</button>
          <button class="link" @click="setMode('signup')">Create account</button>
        </template>
        <template v-else>
          <button class="link" @click="setMode('signin')">Back to sign in</button>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-wrap {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: radial-gradient(1200px 600px at 50% -10%, rgba(79, 70, 229, 0.12), transparent);
}
.auth-card { width: 100%; max-width: 400px; }
.brand { display: flex; align-items: center; gap: 12px; margin-bottom: 24px; }
.brand-mark {
  width: 44px; height: 44px; border-radius: 12px; background: var(--rr-primary);
  color: #fff; display: grid; place-items: center; font-weight: 700;
}
.auth-title { margin-bottom: 20px; }
.google { margin-top: 12px; border: 1px solid var(--rr-border); }
.switches { display: flex; justify-content: space-between; margin-top: 16px; }
.link {
  background: none; border: none; color: var(--rr-primary);
  font: inherit; font-size: 13px; cursor: pointer; padding: 4px;
}
.link:hover { text-decoration: underline; }
.info { color: var(--rr-success); font-size: 13px; margin-top: 8px; }
</style>
