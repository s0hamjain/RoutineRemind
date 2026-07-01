import { Injectable, inject } from '@angular/core';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

export interface AppUser {
  uid: string;
  displayName?: string;
  email?: string;
  role?: 'student' | 'parent' | null;
  linkedUserIds?: string[];
  shareCode?: string;
  photoUrl?: string;
}

export interface LinkedStudent {
  uid: string;
  displayName?: string;
  email?: string;
  shareCode?: string;
  photoUrl?: string;
}

export interface ScheduleItem {
  id: string;
  time: string;
  title: string;
  description?: string;
  icon?: string;
  imageUrl?: string;
  parentNote?: string;
  audioUrl?: string;
  transitionHint?: string;
  sortOrder?: number;
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

export interface ScheduleDraft {
  ownerUid?: string;
  title: string;
  date: string;
  status?: string;
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

export interface ChatRequest {
  ownerUid?: string;
  message: string;
}

export interface ChatResponse {
  answer: string;
  scheduleId: string;
  matchedItem?: ScheduleItem;
  source: 'rules' | 'gemini' | 'fallback' | string;
  createdAt?: string;
}

export interface ChatMessage {
  id: string;
  ownerUid: string;
  question: string;
  answer: string;
  scheduleId?: string;
  matchedItemId?: string;
  source?: string;
  createdAt?: string;
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private auth = inject(AuthService);

  private async request<T>(path: string, options: RequestInit = {}): Promise<T> {
    const token = await this.auth.getIdToken();
    const res = await fetch(`${environment.apiBaseUrl}${path}`, {
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
        /* ignore parse error */
      }
      throw new Error(message);
    }
    if (res.status === 204) {
      return undefined as T;
    }
    return res.json() as Promise<T>;
  }

  createSession(): Promise<AppUser> {
    return this.request<AppUser>('/auth/session', { method: 'POST' });
  }

  me(): Promise<AppUser> {
    return this.request<AppUser>('/me');
  }

  setRole(role: 'student' | 'parent'): Promise<AppUser> {
    return this.request<AppUser>('/me/role', {
      method: 'POST',
      body: JSON.stringify({ role }),
    });
  }

  linkStudent(shareCode: string): Promise<AppUser> {
    return this.request<AppUser>('/me/link', {
      method: 'POST',
      body: JSON.stringify({ shareCode }),
    });
  }

  linkedStudents(): Promise<LinkedStudent[]> {
    return this.request<LinkedStudent[]>('/me/linked-students');
  }

  unlinkStudent(studentUid: string): Promise<AppUser> {
    return this.request<AppUser>(`/me/linked-students/${studentUid}`, {
      method: 'DELETE',
    });
  }

  todaySchedule(ownerUid?: string): Promise<Schedule> {
    const query = ownerUid ? `?ownerUid=${encodeURIComponent(ownerUid)}` : '';
    return this.request<Schedule>(`/schedule/today${query}`);
  }

  schedules(ownerUid?: string): Promise<Schedule[]> {
    const query = ownerUid ? `?ownerUid=${encodeURIComponent(ownerUid)}` : '';
    return this.request<Schedule[]>(`/schedules${query}`);
  }

  createSchedule(draft: ScheduleDraft): Promise<Schedule> {
    return this.request<Schedule>('/schedules', {
      method: 'POST',
      body: JSON.stringify(draft),
    });
  }

  updateSchedule(id: string, draft: ScheduleDraft): Promise<Schedule> {
    return this.request<Schedule>(`/schedules/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(draft),
    });
  }

  completeItem(scheduleId: string, itemId: string): Promise<Schedule> {
    return this.request<Schedule>(`/schedules/${scheduleId}/items/${itemId}/complete`, {
      method: 'POST',
    });
  }

  questions(scheduleId: string): Promise<Question[]> {
    return this.request<Question[]>(`/schedules/${scheduleId}/questions`);
  }

  createQuestion(scheduleId: string, prompt: string): Promise<Question> {
    return this.request<Question>(`/schedules/${scheduleId}/questions`, {
      method: 'POST',
      body: JSON.stringify({ prompt, type: 'text' }),
    });
  }

  responses(questionId: string): Promise<QuestionResponse[]> {
    return this.request<QuestionResponse[]>(`/questions/${questionId}/responses`);
  }

  askChat(request: ChatRequest): Promise<ChatResponse> {
    return this.request<ChatResponse>('/chat', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  chatHistory(ownerUid?: string, limit = 20): Promise<ChatMessage[]> {
    const params = new URLSearchParams();
    if (ownerUid) {
      params.set('ownerUid', ownerUid);
    }
    params.set('limit', String(limit));
    return this.request<ChatMessage[]>(`/chat/history?${params.toString()}`);
  }
}
