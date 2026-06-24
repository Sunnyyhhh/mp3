import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Songs from '../views/Songs.vue'
import Playlists from '../views/Playlists.vue'
import PlaylistDetail from '../views/PlaylistDetail.vue'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: Login },
  { path: '/register', component: Register },
  {
    path: '/songs',
    component: Songs,
    meta: { requiresAuth: true }
  },
  {
    path: '/playlists',
    component: Playlists,
    meta: { requiresAuth: true }
  },
  {
    path: '/playlists/:id',
    component: PlaylistDetail,
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Guard : redirige vers login si pas connecté
router.beforeEach((to, from, next) => {
  const user = localStorage.getItem('user')
  if (to.meta.requiresAuth && !user) {
    next('/login')
  } else {
    next()
  }
})

export default router