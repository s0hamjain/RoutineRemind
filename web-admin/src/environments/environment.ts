/**
 * Client-side config. The Firebase web apiKey/authDomain are NOT secrets
 * (they identify the project; access is controlled by Identity Platform + backend).
 */
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080/api/v1',
  firebase: {
    apiKey: 'REDACTED',
    authDomain: 'routineremind.firebaseapp.com',
    projectId: 'routineremind',
  },
};
