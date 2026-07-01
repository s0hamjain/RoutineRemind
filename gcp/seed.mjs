import admin from 'firebase-admin';
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const serviceAccountPath = resolve(__dirname, '../secrets/routineremind-f3d2edfe549c.json');
const serviceAccount = JSON.parse(readFileSync(serviceAccountPath, 'utf8'));

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: 'routineremind',
  storageBucket: 'routineremind-media',
});

const db = admin.firestore();

const studentUid = process.argv[2];
const mode = studentUid ? 'real-user' : 'demo-user';
const uid = studentUid ?? 'demo-student';
const today = new Date().toISOString().slice(0, 10);

const sampleItems = [
  {
    id: 'wake-up',
    time: '07:30',
    title: 'Wake up and stretch',
    description: 'Start with a calm stretch routine.',
    completed: false,
    completedAt: null,
  },
  {
    id: 'breakfast',
    time: '08:00',
    title: 'Eat breakfast',
    description: 'Choose one healthy breakfast option.',
    completed: false,
    completedAt: null,
  },
  {
    id: 'school-bag',
    time: '08:30',
    title: 'Pack school bag',
    description: 'Check homework, water bottle, and lunch.',
    completed: false,
    completedAt: null,
  },
];

async function main() {
  const studentRef = db.collection('users').doc(uid);
  const studentSnap = await studentRef.get();

  if (!studentSnap.exists) {
    await studentRef.set({
      displayName: mode === 'demo-user' ? 'Demo Student' : '',
      email: mode === 'demo-user' ? 'demo-student@example.com' : '',
      role: 'student',
      linkedUserIds: [],
      shareCode: 'DEMO42',
      photoUrl: null,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    console.log(`Created ${mode} profile: users/${uid}`);
  } else {
    await studentRef.set(
      {
        role: 'student',
        shareCode: studentSnap.get('shareCode') ?? 'DEMO42',
        linkedUserIds: studentSnap.get('linkedUserIds') ?? [],
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      },
      { merge: true },
    );
    console.log(`Updated existing student profile: users/${uid}`);
  }

  const scheduleId = `${uid}-${today}`;
  await db.collection('schedules').doc(scheduleId).set(
    {
      ownerUid: uid,
      date: today,
      title: 'Morning Routine',
      status: 'active',
      items: sampleItems,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    },
    { merge: true },
  );

  console.log(`Seeded today's schedule: schedules/${scheduleId}`);
  console.log('');
  console.log('Next steps:');
  if (mode === 'demo-user') {
    console.log('- This used uid "demo-student"; use a real Identity Platform uid for login testing.');
    console.log('- Run: npm run seed -- <student-uid>');
  } else {
    console.log('- Sign in as that student and open Today to see the seeded schedule.');
    console.log('- Parent linking code is the student shareCode shown in users/{uid}.');
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
