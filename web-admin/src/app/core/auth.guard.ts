import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { auth } from './firebase';

/**
 * Waits for Firebase to resolve the initial auth state, then allows or
 * redirects to /login.
 */
export const authGuard: CanActivateFn = async () => {
  const router = inject(Router);

  const user = await new Promise((resolve) => {
    const unsub = auth.onAuthStateChanged((u) => {
      unsub();
      resolve(u);
    });
  });

  if (user) {
    return true;
  }
  return router.createUrlTree(['/login']);
};
