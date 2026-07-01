import {
  GoogleAuthProvider,
  createUserWithEmailAndPassword,
  onAuthStateChanged,
  sendPasswordResetEmail,
  signInWithEmailAndPassword,
  signInWithPopup,
  signOut,
  type User,
} from 'firebase/auth';
import { ref } from 'vue';
import { auth } from '../firebase';

export const currentUser = ref<User | null>(null);
export const authReady = ref(false);

onAuthStateChanged(auth, (user) => {
  currentUser.value = user;
  authReady.value = true;
});

export function waitForAuth(): Promise<User | null> {
  return new Promise((resolve) => {
    const unsub = onAuthStateChanged(auth, (u) => {
      unsub();
      resolve(u);
    });
  });
}

export const authService = {
  signIn: (email: string, password: string) => signInWithEmailAndPassword(auth, email, password),
  signUp: (email: string, password: string) => createUserWithEmailAndPassword(auth, email, password),
  signInWithGoogle: () => signInWithPopup(auth, new GoogleAuthProvider()),
  resetPassword: (email: string) => sendPasswordResetEmail(auth, email),
  signOut: () => signOut(auth),
  getIdToken: () => (auth.currentUser ? auth.currentUser.getIdToken() : Promise.resolve(null)),
};
