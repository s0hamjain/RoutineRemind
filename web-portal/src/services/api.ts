import { config } from '../config';
import { authService } from './auth';

export interface AppUser {
  uid: string;
  displayName?: string;
  email?: string;
  role?: 'student' | 'parent' | null;
  linkedUserIds?: string[];
  shareCode?: string;
  photoUrl?: string;
}

export interface ScheduleItem {
  id: string;
  time: string;
  title: string;
  description?: string;
  completed: boolean;
  completedAt?: string;
}

export interface Schedule {
  id: string;
  ownerUid: string;
  date: string;
  title: string;
  status: string;
  items: ScheduleItem[];
}

export interface Question {
  id: string;
  scheduleId: string;
  ownerUid: string;
  prompt: string;
  type: 'text' | 'audio' | 'video';
  order: number;
}

export interface QuestionResponse {
  id: string;
  questionId: string;
  studentUid: string;
  text?: string;
  mediaUrl?: string;
  transcript?: string;
  createdAt?: string;
}

export interface MediaUpload {
  responseId: string;
  uploadUrl: string;
  objectName: string;
  mediaUrl: string;
  contentType: string;
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = await authService.getIdToken();
  const res = await fetch(`${config.apiBaseUrl}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers || {}),
    },
  });
  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    try {
      const body = await res.json();
      message = body?.error?.message ?? message;
    } catch {
      /* ignore */
    }
    throw new Error(message);
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

export const api = {
  createSession: () => request<AppUser>('/auth/session', { method: 'POST' }),
  me: () => request<AppUser>('/me'),
  setRole: (role: 'student' | 'parent') =>
    request<AppUser>('/me/role', { method: 'POST', body: JSON.stringify({ role }) }),
  linkStudent: (shareCode: string) =>
    request<AppUser>('/me/link', { method: 'POST', body: JSON.stringify({ shareCode }) }),
  todaySchedule: () => request<Schedule>('/schedule/today'),
  completeItem: (scheduleId: string, itemId: string) =>
    request<Schedule>(`/schedules/${scheduleId}/items/${itemId}/complete`, { method: 'POST' }),
  questions: (scheduleId: string) => request<Question[]>(`/schedules/${scheduleId}/questions`),
  submitTextResponse: (questionId: string, text: string) =>
    request<QuestionResponse>(`/questions/${questionId}/responses`, {
      method: 'POST',
      body: JSON.stringify({ text }),
    }),
  createMediaUpload: (questionId: string, contentType: string) =>
    request<MediaUpload>(`/questions/${questionId}/responses/media`, {
      method: 'POST',
      body: JSON.stringify({ contentType }),
    }),
  transcribeResponse: (responseId: string) =>
    request<QuestionResponse>(`/responses/${responseId}/transcribe`, {
      method: 'POST',
      body: JSON.stringify({ languageCode: 'en-US' }),
    }),
};
