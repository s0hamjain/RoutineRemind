import { createRouter, createWebHistory } from 'vue-router';
import { waitForAuth } from './services/auth';
import HomeView from './views/HomeView.vue';
import LoginView from './views/LoginView.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView },
    { path: '/', name: 'home', component: HomeView, meta: { requiresAuth: true } },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
});

router.beforeEach(async (to) => {
  if (to.meta.requiresAuth) {
    const user = await waitForAuth();
    if (!user) {
      return { name: 'login' };
    }
  }
  return true;
});

export default router;
