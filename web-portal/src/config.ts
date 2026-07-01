// Client-side config. Firebase web apiKey/authDomain are not secrets.
export const config = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1',
  firebase: {
    apiKey: 'REDACTED',
    authDomain: 'routineremind.firebaseapp.com',
    projectId: 'routineremind',
  },
};
