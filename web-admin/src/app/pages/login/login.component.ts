import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';

type Mode = 'signin' | 'signup' | 'reset';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="auth-wrap">
      <div class="rr-card auth-card">
        <div class="brand">
          <div class="brand-mark">RR</div>
          <div>
            <h1>RoutineRemind</h1>
            <p class="rr-caption">Visual routines and schedule chat for families</p>
          </div>
        </div>

        <h2 class="auth-title">
          {{ mode() === 'signin' ? 'Welcome back' : mode() === 'signup' ? 'Create your account' : 'Reset password' }}
        </h2>

        <div class="rr-field">
          <label class="rr-label" for="email">Email</label>
          <input id="email" class="rr-input" type="email" [(ngModel)]="email" placeholder="you@example.com" />
        </div>

        @if (mode() !== 'reset') {
          <div class="rr-field">
            <label class="rr-label" for="password">Password</label>
            <input id="password" class="rr-input" type="password" [(ngModel)]="password" placeholder="••••••••" />
          </div>
        }

        @if (error()) {
          <div class="rr-error">{{ error() }}</div>
        }
        @if (info()) {
          <div class="info">{{ info() }}</div>
        }

        <button class="rr-btn rr-btn-primary rr-btn-block" [disabled]="busy()" (click)="submit()">
          {{ busy() ? 'Please wait…' : primaryLabel() }}
        </button>

        @if (mode() !== 'reset') {
          <button class="rr-btn rr-btn-ghost rr-btn-block google" [disabled]="busy()" (click)="google()">
            Continue with Google
          </button>
        }

        <div class="switches">
          @if (mode() === 'signin') {
            <button class="link" (click)="setMode('reset')">Forgot password?</button>
            <button class="link" (click)="setMode('signup')">Create account</button>
          } @else {
            <button class="link" (click)="setMode('signin')">Back to sign in</button>
          }
        </div>
      </div>
    </div>
  `,
  styles: [
    `
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
        width: 44px; height: 44px; border-radius: 12px;
        background: var(--rr-primary); color: #fff;
        display: grid; place-items: center; font-weight: 700;
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
    `,
  ],
})
export class LoginComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  email = '';
  password = '';
  mode = signal<Mode>('signin');
  busy = signal(false);
  error = signal('');
  info = signal('');

  setMode(m: Mode) {
    this.mode.set(m);
    this.error.set('');
    this.info.set('');
  }

  primaryLabel(): string {
    switch (this.mode()) {
      case 'signup':
        return 'Create account';
      case 'reset':
        return 'Send reset link';
      default:
        return 'Sign in';
    }
  }

  async submit() {
    this.error.set('');
    this.info.set('');
    this.busy.set(true);
    try {
      if (this.mode() === 'signin') {
        await this.authService.signIn(this.email, this.password);
        await this.router.navigate(['/']);
      } else if (this.mode() === 'signup') {
        await this.authService.signUp(this.email, this.password);
        await this.router.navigate(['/']);
      } else {
        await this.authService.resetPassword(this.email);
        this.info.set('Password reset email sent. Check your inbox.');
      }
    } catch (e: any) {
      this.error.set(this.friendly(e?.code ?? e?.message));
    } finally {
      this.busy.set(false);
    }
  }

  async google() {
    this.error.set('');
    this.busy.set(true);
    try {
      await this.authService.signInWithGoogle();
      await this.router.navigate(['/']);
    } catch (e: any) {
      this.error.set(this.friendly(e?.code ?? e?.message));
    } finally {
      this.busy.set(false);
    }
  }

  private friendly(code: string): string {
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
}
