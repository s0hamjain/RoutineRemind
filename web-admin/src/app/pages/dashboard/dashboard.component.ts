import { NgTemplateOutlet } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  ApiService,
  AppUser,
  ChatMessage,
  ChatResponse,
  LinkedStudent,
  Schedule,
  ScheduleItem,
} from '../../core/api.service';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [FormsModule, NgTemplateOutlet],
  template: `
    <header class="topbar">
      <div class="brand">
        <div class="brand-mark">RR</div>
        <strong>RoutineRemind</strong>
        <span class="rr-badge">Family Portal</span>
      </div>
      <div class="user">
        @if (user()) {
          <span class="rr-caption">{{ user()!.email }}</span>
        }
        <button class="rr-btn rr-btn-ghost" (click)="signOut()">Sign out</button>
      </div>
    </header>

    <main class="content">
      @if (loading()) {
        <div class="rr-card skeleton">Loading…</div>
      } @else if (error()) {
        <div class="rr-card"><div class="rr-error">{{ error() }}</div></div>
      } @else if (!user()?.role) {
        <!-- Role selection -->
        <div class="rr-card role-card">
          <h1>Welcome to RoutineRemind</h1>
          <p class="rr-caption">Choose how this account will use the portal. You can create routines as a parent or use a calm child view for the daily schedule.</p>
          <div class="role-grid">
            <button class="role-option" (click)="chooseRole('parent')">
              <span class="emoji">👨‍👩‍👧</span>
              <strong>I'm a Parent</strong>
              <span class="rr-caption">Create visual routines and review child questions.</span>
            </button>
            <button class="role-option" (click)="chooseRole('student')">
              <span class="emoji">🎓</span>
              <strong>I'm a Student</strong>
              <span class="rr-caption">See my day and ask questions about what comes next.</span>
            </button>
          </div>
        </div>
      } @else if (user()?.role === 'parent' && !linkedStudents().length) {
        <!-- Parent: link a student -->
        <div class="rr-card role-card">
          <h1>Link a student</h1>
          <p class="rr-caption">Enter the 6-character share code from your student's app.</p>
          <div class="rr-field">
            <label class="rr-label" for="code">Share code</label>
            <input id="code" class="rr-input" [(ngModel)]="shareCode" placeholder="e.g. K7M2QP" maxlength="8" />
          </div>
          @if (linkError()) { <div class="rr-error">{{ linkError() }}</div> }
          <button class="rr-btn rr-btn-primary" [disabled]="linking()" (click)="linkStudent()">
            {{ linking() ? 'Linking…' : 'Link student' }}
          </button>
        </div>
      } @else if (user()?.role === 'student') {
        <!-- Student view: share code + today -->
        <div class="rr-card">
          <span class="rr-label">Your share code</span>
          <div class="share-code">{{ user()?.shareCode }}</div>
          <p class="rr-caption">Give this to your parent so they can link to your account.</p>
        </div>
        <ng-container *ngTemplateOutlet="childChat" />
        <ng-container *ngTemplateOutlet="todayBlock" />
      } @else {
        <!-- Parent with linked student -->
        <ng-container *ngTemplateOutlet="studentSwitcher" />
        <ng-container *ngTemplateOutlet="scheduleManager" />
        <ng-container *ngTemplateOutlet="chatHistoryBlock" />
        <ng-container *ngTemplateOutlet="todayBlock" />
      }
    </main>

    <ng-template #studentSwitcher>
      <section class="rr-card student-switcher">
        <div>
          <span class="rr-label">Selected student</span>
          @if (selectedStudent()) {
            <h2>{{ studentLabel(selectedStudent()!) }}</h2>
            <p class="rr-caption">{{ selectedStudent()?.email || 'No email on profile' }}</p>
          }
        </div>
        <div class="switcher-controls">
          <select class="rr-input" [ngModel]="selectedStudentUid()" (ngModelChange)="selectStudent($event)">
            @for (student of linkedStudents(); track student.uid) {
              <option [value]="student.uid">{{ studentLabel(student) }}</option>
            }
          </select>
          <button class="rr-btn rr-btn-ghost" [disabled]="unlinking()" (click)="unlinkSelectedStudent()">
            {{ unlinking() ? 'Unlinking…' : 'Unlink' }}
          </button>
        </div>
      </section>
    </ng-template>

    <ng-template #scheduleManager>
      <section class="manager">
        <div class="today-head">
          <h1>Manage schedules</h1>
          <button class="rr-btn rr-btn-ghost" (click)="resetForm()">New schedule</button>
        </div>

        <div class="rr-card">
          <div class="form-grid">
            <div class="rr-field">
              <label class="rr-label" for="schedule-title">Title</label>
              <input id="schedule-title" class="rr-input" [(ngModel)]="formTitle" placeholder="Morning Routine" />
            </div>
            <div class="rr-field">
              <label class="rr-label" for="schedule-date">Date</label>
              <input id="schedule-date" class="rr-input" type="date" [(ngModel)]="formDate" />
            </div>
          </div>

          <div class="items-header">
            <span class="rr-label">Items</span>
            <button class="rr-btn rr-btn-ghost compact" (click)="addFormItem()">Add item</button>
          </div>

          <div class="form-items">
            @for (item of formItems; track item.id) {
              <div class="form-item">
                <input class="rr-input time-input" [(ngModel)]="item.time" placeholder="08:00" />
                <input class="rr-input icon-input" [(ngModel)]="item.icon" placeholder="icon" />
                <input class="rr-input" [(ngModel)]="item.title" placeholder="Task title" />
                <button class="rr-btn rr-btn-ghost compact" (click)="removeFormItem(item.id)">Remove</button>
                <input class="rr-input item-wide" [(ngModel)]="item.description" placeholder="Short child-facing description" />
                <input class="rr-input item-wide" [(ngModel)]="item.parentNote" placeholder="Parent note the chatbot can use" />
                <input class="rr-input item-wide" [(ngModel)]="item.transitionHint" placeholder="Transition hint, e.g. Then put on shoes." />
              </div>
            }
          </div>

          @if (saveError()) { <div class="rr-error">{{ saveError() }}</div> }
          <button class="rr-btn rr-btn-primary" [disabled]="saving()" (click)="saveSchedule()">
            {{ saving() ? 'Saving…' : editingScheduleId() ? 'Update schedule' : 'Create schedule' }}
          </button>
        </div>

        <div class="schedule-list">
          @if (scheduleList().length) {
            @for (s of scheduleList(); track s.id) {
              <button class="schedule-list-item" (click)="editSchedule(s)">
                <div>
                  <strong>{{ s.title }}</strong>
                  <span class="rr-caption">{{ s.date }} · {{ s.items.length }} items</span>
                </div>
                <span class="rr-badge">{{ s.status }}</span>
              </button>
            }
          } @else {
            <div class="rr-card empty small">
              <strong>No schedules yet</strong>
              <p class="rr-caption">Create the first schedule for your linked student.</p>
            </div>
          }
        </div>

        <div class="rr-card preview-card">
          <span class="rr-label">Child preview</span>
          <h2>{{ formTitle || 'Today' }}</h2>
          <div class="visual-list">
            @for (item of formItems; track item.id) {
              @if (item.title) {
                <div class="visual-item">
                  <span class="visual-icon">{{ item.icon || 'star' }}</span>
                  <div>
                    <strong>{{ item.title }}</strong>
                    @if (item.transitionHint) { <p class="rr-caption">{{ item.transitionHint }}</p> }
                  </div>
                </div>
              }
            }
          </div>
        </div>
      </section>
    </ng-template>

    <ng-template #todayBlock>
      <section class="today">
        <div class="today-head">
          <h1>Today's schedule</h1>
          <span class="rr-caption">{{ todayLabel }}</span>
        </div>
        @if (schedule()) {
          <div class="rr-card">
            <div class="sched-title">
              <h2>{{ schedule()!.title }}</h2>
              <span class="rr-badge">{{ schedule()!.status }}</span>
            </div>
            <ul class="items visual-schedule">
              @for (item of schedule()!.items; track item.id) {
                <li class="item" [class.done]="item.completed">
                  <span class="visual-icon">{{ item.icon || 'star' }}</span>
                  <span class="time">{{ item.time }}</span>
                  <div class="item-body">
                    <strong>{{ item.title }}</strong>
                    @if (item.description) { <span class="rr-caption">{{ item.description }}</span> }
                    @if (item.transitionHint) { <span class="rr-caption">{{ item.transitionHint }}</span> }
                  </div>
                  @if (user()?.role === 'student' && !item.completed) {
                    <button class="rr-btn rr-btn-primary compact" [disabled]="completingItemId() === item.id" (click)="completeItem(item.id)">
                      {{ completingItemId() === item.id ? 'Saving…' : 'Done' }}
                    </button>
                  } @else {
                    <span class="status-dot" [class.done]="item.completed"></span>
                  }
                </li>
              }
            </ul>
          </div>
        } @else {
          <div class="rr-card empty">
            <span class="emoji">📅</span>
            <strong>No schedule for today</strong>
            <p class="rr-caption">Schedules you create will appear here.</p>
          </div>
        }
      </section>
    </ng-template>

    <ng-template #childChat>
      <section class="rr-card chat-card">
        <span class="rr-label">Ask about my day</span>
        <h1>What do you want to know?</h1>
        <div class="quick-prompts">
          <button class="quick-prompt" (click)="askQuick('What do I do now?')">What now?</button>
          <button class="quick-prompt" (click)="askQuick('What comes next?')">What next?</button>
          <button class="quick-prompt" (click)="askQuick('When do I eat?')">When do I eat?</button>
          <button class="quick-prompt" (click)="askQuick('I need help.')">I need help</button>
        </div>
        <div class="chat-input-row">
          <input class="rr-input" [(ngModel)]="chatInput" placeholder="Ask a question about your schedule" />
          <button class="rr-btn rr-btn-primary" [disabled]="askingChat()" (click)="askChat()">
            {{ askingChat() ? 'Thinking…' : 'Ask' }}
          </button>
        </div>
        @if (chatError()) { <div class="rr-error">{{ chatError() }}</div> }
        @if (chatAnswer()) {
          <div class="chat-answer">
            <span class="rr-label">RoutineRemind says</span>
            <p>{{ chatAnswer()!.answer }}</p>
            @if (chatAnswer()!.matchedItem) {
              <span class="rr-caption">About: {{ chatAnswer()!.matchedItem!.title }}</span>
            }
          </div>
        }
      </section>
    </ng-template>

    <ng-template #chatHistoryBlock>
      <section class="rr-card chat-card">
        <div class="today-head">
          <div>
            <span class="rr-label">Child questions</span>
            <h2>Recent chat</h2>
          </div>
          <button class="rr-btn rr-btn-ghost compact" (click)="loadChatHistory()">Refresh</button>
        </div>
        @for (message of chatHistory(); track message.id) {
          <div class="history-row">
            <strong>{{ message.question }}</strong>
            <p>{{ message.answer }}</p>
            <span class="rr-caption">{{ message.createdAt || 'Just now' }} · {{ message.source || 'chat' }}</span>
          </div>
        } @empty {
          <p class="rr-caption">No child questions yet. They will appear here after the child uses chat.</p>
        }
      </section>
    </ng-template>
  `,
  styles: [
    `
      .topbar {
        display: flex; align-items: center; justify-content: space-between;
        padding: 16px 24px; background: var(--rr-surface);
        border-bottom: 1px solid var(--rr-border);
      }
      .brand { display: flex; align-items: center; gap: 10px; }
      .brand-mark {
        width: 32px; height: 32px; border-radius: 8px; background: var(--rr-primary);
        color: #fff; display: grid; place-items: center; font-weight: 700; font-size: 13px;
      }
      .user { display: flex; align-items: center; gap: 12px; }
      .content { max-width: 720px; margin: 0 auto; padding: 32px 24px; display: flex; flex-direction: column; gap: 24px; }
      .role-card h1 { margin-bottom: 4px; }
      .role-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 20px; }
      .role-option {
        display: flex; flex-direction: column; gap: 6px; text-align: left;
        padding: 20px; border: 1px solid var(--rr-border); border-radius: var(--rr-radius-md);
        background: #fff; cursor: pointer; transition: border-color 0.15s, box-shadow 0.15s;
      }
      .role-option:hover { border-color: var(--rr-primary); box-shadow: var(--rr-shadow-md); }
      .emoji { font-size: 28px; }
      .share-code {
        font-size: 28px; font-weight: 700; letter-spacing: 0.15em;
        color: var(--rr-primary); margin: 8px 0;
      }
      .student-switcher {
        display: flex; align-items: center; justify-content: space-between; gap: 16px;
      }
      .student-switcher h2 { margin-top: 4px; }
      .switcher-controls { display: flex; align-items: center; gap: 10px; min-width: 320px; }
      .switcher-controls select { width: 100%; }
      .today-head { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 12px; }
      .manager { display: flex; flex-direction: column; gap: 16px; }
      .form-grid { display: grid; grid-template-columns: 1fr 180px; gap: 16px; }
      .items-header { display: flex; align-items: center; justify-content: space-between; margin: 8px 0 12px; }
      .form-items { display: flex; flex-direction: column; gap: 10px; margin-bottom: 16px; }
      .form-item { display: grid; grid-template-columns: 92px 90px 1fr auto; gap: 8px; align-items: center; }
      .time-input { text-align: center; }
      .icon-input { text-align: center; }
      .item-wide { grid-column: span 2; }
      .compact { padding: 9px 12px; font-size: 13px; }
      .schedule-list { display: flex; flex-direction: column; gap: 8px; }
      .schedule-list-item {
        display: flex; align-items: center; justify-content: space-between; gap: 16px;
        width: 100%; text-align: left; padding: 14px 16px; background: var(--rr-surface);
        border: 1px solid var(--rr-border); border-radius: var(--rr-radius-md);
        cursor: pointer;
      }
      .schedule-list-item:hover { border-color: var(--rr-primary); box-shadow: var(--rr-shadow-sm); }
      .preview-card { display: flex; flex-direction: column; gap: 12px; }
      .visual-list { display: flex; flex-direction: column; gap: 10px; }
      .visual-item {
        display: flex; align-items: center; gap: 14px; padding: 14px;
        border-radius: var(--rr-radius-md); background: #f8fafc; border: 1px solid var(--rr-border);
      }
      .visual-icon {
        width: 48px; height: 48px; border-radius: 16px; display: grid; place-items: center;
        background: rgba(79, 70, 229, 0.1); color: var(--rr-primary); font-weight: 800;
      }
      .sched-title { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
      .items { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; }
      .item {
        display: flex; align-items: center; gap: 16px; padding: 14px;
        border: 1px solid var(--rr-border); border-radius: var(--rr-radius-md);
      }
      .item.done { opacity: 0.6; }
      .item.done strong { text-decoration: line-through; }
      .time { font-weight: 600; color: var(--rr-primary); min-width: 56px; }
      .item-body { display: flex; flex-direction: column; flex: 1; }
      .status-dot {
        width: 12px; height: 12px; border-radius: 50%;
        border: 2px solid var(--rr-border);
      }
      .status-dot.done { background: var(--rr-success); border-color: var(--rr-success); }
      .empty { text-align: center; display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 40px; }
      .empty.small { padding: 24px; }
      .chat-card { display: flex; flex-direction: column; gap: 16px; }
      .quick-prompts { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
      .quick-prompt {
        border: 1px solid var(--rr-border); border-radius: var(--rr-radius-md);
        background: #eef2ff; color: var(--rr-primary-dark); font-weight: 700;
        min-height: 64px; cursor: pointer; font: inherit;
      }
      .chat-input-row { display: grid; grid-template-columns: 1fr auto; gap: 10px; }
      .chat-answer, .history-row {
        background: #f8fafc; border: 1px solid var(--rr-border);
        border-radius: var(--rr-radius-md); padding: 16px;
      }
      .chat-answer p, .history-row p { margin: 6px 0; }
      .skeleton { color: var(--rr-text-secondary); }
      @media (max-width: 720px) {
        .form-grid, .form-item, .quick-prompts, .chat-input-row { grid-template-columns: 1fr; }
        .item-wide { grid-column: span 1; }
        .student-switcher, .switcher-controls { align-items: stretch; flex-direction: column; min-width: 0; }
      }
    `,
  ],
})
export class DashboardComponent {
  private api = inject(ApiService);
  private authService = inject(AuthService);
  private router = inject(Router);

  user = signal<AppUser | null>(null);
  schedule = signal<Schedule | null>(null);
  loading = signal(true);
  error = signal('');
  shareCode = '';
  linking = signal(false);
  unlinking = signal(false);
  linkError = signal('');
  linkedStudents = signal<LinkedStudent[]>([]);
  selectedStudentUid = signal('');
  scheduleList = signal<Schedule[]>([]);
  editingScheduleId = signal<string | null>(null);
  saving = signal(false);
  saveError = signal('');
  chatInput = '';
  askingChat = signal(false);
  chatError = signal('');
  chatAnswer = signal<ChatResponse | null>(null);
  chatHistory = signal<ChatMessage[]>([]);
  completingItemId = signal('');
  formTitle = 'Morning Routine';
  formDate = new Date().toISOString().slice(0, 10);
  formItems: ScheduleItem[] = [
    { id: this.newId(), time: '07:30', title: 'Wake up and stretch', description: '', icon: 'sun', parentNote: '', transitionHint: 'Start slowly.', completed: false },
    { id: this.newId(), time: '08:00', title: 'Eat breakfast', description: '', icon: 'food', parentNote: '', transitionHint: '', completed: false },
  ];

  readonly todayLabel = new Date().toLocaleDateString(undefined, {
    weekday: 'long',
    month: 'long',
    day: 'numeric',
  });

  constructor() {
    this.bootstrap();
  }

  private async bootstrap() {
    this.loading.set(true);
    this.error.set('');
    try {
      const profile = await this.api.createSession();
      this.user.set(profile);
      if (profile.role === 'parent' && profile.linkedUserIds?.length) {
        await this.loadLinkedStudents();
        await this.loadToday();
        await this.loadSchedules();
        await this.loadChatHistory();
      } else if (profile.role === 'student') {
        await this.loadToday();
      }
    } catch (e: any) {
      this.error.set(e?.message ?? 'Failed to load your account.');
    } finally {
      this.loading.set(false);
    }
  }

  private async loadToday() {
    try {
      const ownerUid = this.user()?.role === 'parent' ? this.selectedStudentUid() : undefined;
      this.schedule.set(await this.api.todaySchedule(ownerUid));
    } catch {
      // 404 (no schedule today) → empty state.
      this.schedule.set(null);
    }
  }

  async chooseRole(role: 'parent' | 'student') {
    this.loading.set(true);
    try {
      this.user.set(await this.api.setRole(role));
      if (role === 'student') {
        await this.loadToday();
      }
    } catch (e: any) {
      this.error.set(e?.message ?? 'Failed to set role.');
    } finally {
      this.loading.set(false);
    }
  }

  async linkStudent() {
    this.linkError.set('');
    this.linking.set(true);
    try {
      this.user.set(await this.api.linkStudent(this.shareCode));
      await this.loadLinkedStudents();
      await this.loadToday();
      await this.loadSchedules();
      await this.loadChatHistory();
    } catch (e: any) {
      this.linkError.set(e?.message ?? 'Could not link student.');
    } finally {
      this.linking.set(false);
    }
  }

  private async loadLinkedStudents() {
    const students = await this.api.linkedStudents();
    this.linkedStudents.set(students);
    if (!students.some((student) => student.uid === this.selectedStudentUid())) {
      this.selectedStudentUid.set(students[0]?.uid ?? '');
    }
  }

  private async loadSchedules() {
    try {
      const ownerUid = this.selectedStudentUid();
      this.scheduleList.set(await this.api.schedules(ownerUid));
    } catch {
      this.scheduleList.set([]);
    }
  }

  async selectStudent(studentUid: string) {
    this.selectedStudentUid.set(studentUid);
    this.resetForm();
    await this.loadToday();
    await this.loadSchedules();
    await this.loadChatHistory();
  }

  selectedStudent(): LinkedStudent | undefined {
    return this.linkedStudents().find((student) => student.uid === this.selectedStudentUid());
  }

  studentLabel(student: LinkedStudent): string {
    return student.displayName?.trim() || student.email?.trim() || student.shareCode || student.uid;
  }

  async unlinkSelectedStudent() {
    const uid = this.selectedStudentUid();
    if (!uid) {
      return;
    }
    this.unlinking.set(true);
    this.linkError.set('');
    try {
      this.user.set(await this.api.unlinkStudent(uid));
      this.schedule.set(null);
      this.scheduleList.set([]);
      await this.loadLinkedStudents();
      if (this.selectedStudentUid()) {
        await this.loadToday();
        await this.loadSchedules();
      }
    } catch (e: any) {
      this.linkError.set(e?.message ?? 'Could not unlink student.');
    } finally {
      this.unlinking.set(false);
    }
  }

  addFormItem() {
    this.formItems = [
      ...this.formItems,
      { id: this.newId(), time: '', title: '', description: '', icon: 'star', parentNote: '', transitionHint: '', completed: false },
    ];
  }

  removeFormItem(id: string) {
    this.formItems = this.formItems.filter((item) => item.id !== id);
  }

  editSchedule(schedule: Schedule) {
    this.editingScheduleId.set(schedule.id);
    this.formTitle = schedule.title;
    this.formDate = schedule.date;
    this.formItems = schedule.items.map((item) => ({ ...item }));
    this.saveError.set('');
  }

  resetForm() {
    this.editingScheduleId.set(null);
    this.formTitle = 'Morning Routine';
    this.formDate = new Date().toISOString().slice(0, 10);
    this.formItems = [
      { id: this.newId(), time: '07:30', title: '', description: '', icon: 'star', parentNote: '', transitionHint: '', completed: false },
    ];
    this.saveError.set('');
  }

  async saveSchedule() {
    this.saveError.set('');
    const ownerUid = this.selectedStudentUid();
    if (!ownerUid) {
      this.saveError.set('Link a student before creating schedules.');
      return;
    }
    const items = this.formItems.filter((item) => item.title.trim());
    if (!this.formTitle.trim()) {
      this.saveError.set('Title is required.');
      return;
    }
    if (!items.length) {
      this.saveError.set('Add at least one schedule item.');
      return;
    }

    this.saving.set(true);
    try {
      const draft = {
        ownerUid,
        title: this.formTitle,
        date: this.formDate,
        status: this.editingScheduleId() ? 'active' : 'upcoming',
        items,
      };
      const saved = this.editingScheduleId()
        ? await this.api.updateSchedule(this.editingScheduleId()!, draft)
        : await this.api.createSchedule(draft);

      this.editingScheduleId.set(saved.id);
      await this.loadSchedules();
      await this.loadToday();
      await this.loadChatHistory();
    } catch (e: any) {
      this.saveError.set(e?.message ?? 'Could not save schedule.');
    } finally {
      this.saving.set(false);
    }
  }

  async askQuick(message: string) {
    this.chatInput = message;
    await this.askChat();
  }

  async askChat() {
    if (!this.chatInput.trim()) {
      this.chatError.set('Type a question first.');
      return;
    }
    this.askingChat.set(true);
    this.chatError.set('');
    try {
      const response = await this.api.askChat({ message: this.chatInput.trim() });
      this.chatAnswer.set(response);
      this.chatInput = '';
      await this.loadToday();
    } catch (e: any) {
      this.chatError.set(e?.message ?? 'Could not answer that question.');
    } finally {
      this.askingChat.set(false);
    }
  }

  async loadChatHistory() {
    const ownerUid = this.user()?.role === 'parent' ? this.selectedStudentUid() : undefined;
    if (this.user()?.role === 'parent' && !ownerUid) {
      this.chatHistory.set([]);
      return;
    }
    try {
      this.chatHistory.set(await this.api.chatHistory(ownerUid, 20));
    } catch {
      this.chatHistory.set([]);
    }
  }

  async completeItem(itemId: string) {
    const current = this.schedule();
    if (!current) {
      return;
    }
    this.completingItemId.set(itemId);
    try {
      this.schedule.set(await this.api.completeItem(current.id, itemId));
    } finally {
      this.completingItemId.set('');
    }
  }

  async signOut() {
    await this.authService.signOut();
    await this.router.navigate(['/login']);
  }

  private newId(): string {
    return globalThis.crypto?.randomUUID?.() ?? Math.random().toString(36).slice(2);
  }
}
