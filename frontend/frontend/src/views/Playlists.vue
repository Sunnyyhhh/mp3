<template>
  <div class="page">
    <nav class="navbar">
      <div class="nav-logo"> MP3 Manager</div>
      <div class="nav-links">
        <RouterLink to="/songs">Chansons</RouterLink>
        <RouterLink to="/playlists">Playlists</RouterLink>
      </div>
      <div class="nav-user">
        <span>{{ auth.user?.nom }}</span>
        <button @click="logout">Déconnexion</button>
      </div>
    </nav>

    <div class="content">
      <h2>Mes Playlists</h2>

      <div class="create-card">
        <h3>➕ Créer une playlist</h3>
        <div class="form-row">
          <div class="field">
            <label>Nom</label>
            <input v-model="newNom" placeholder="Ma playlist..." />
          </div>
          <div class="field">
            <label>Durée cible (minutes)</label>
            <input v-model.number="newDuree" type="number" min="1" placeholder="32" />
          </div>
          <button @click="create" :disabled="!newNom || !newDuree">Créer</button>
        </div>
      </div>

      <div class="playlists-grid">
        <div
          v-for="pl in playlists"
          :key="pl.id"
          class="pl-card"
          @click="goTo(pl.id)"
        >
          <div class="pl-icon">🎶</div>
          <div class="pl-info">
            <div class="pl-name">{{ pl.nom }}</div>
            <div class="pl-meta">Durée cible : {{ formatDuree(pl.dureeCible) }}</div>
          </div>
          <button class="btn-delete" @click.stop="remove(pl.id)">🗑</button>
        </div>

        <div v-if="playlists.length === 0" class="empty">
          Aucune playlist — crée-en une !
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { getPlaylists, createPlaylist, deletePlaylist } from '../services/api'

const router = useRouter()
const auth = useAuthStore()

const playlists = ref([])
const newNom = ref('')
const newDuree = ref(32)

onMounted(async () => {
  const res = await getPlaylists()
  playlists.value = res.data
})

async function create() {
  const dureeSecondes = newDuree.value * 60
  await createPlaylist(newNom.value, dureeSecondes, auth.user.id)
  newNom.value = ''
  newDuree.value = 32
  const res = await getPlaylists()
  playlists.value = res.data
}

async function remove(id) {
  if (!confirm('Supprimer cette playlist ?')) return
  await deletePlaylist(id)
  playlists.value = playlists.value.filter(p => p.id !== id)
}

function goTo(id) {
  router.push(`/playlists/${id}`)
}

function formatDuree(sec) {
  if (!sec) return '—'
  const m = Math.floor(sec / 60)
  return `${m} min`
}

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f7fb;
}

/* ================= NAVBAR ================= */

.navbar {
  height: 72px;
  background: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 40px;
  box-shadow: 0 3px 12px rgba(0,0,0,.05);
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-logo {
  font-size: 22px;
  font-weight: 700;
  color: #4f46e5;
}

.nav-links {
  display: flex;
  gap: 30px;
}

.nav-links a {
  text-decoration: none;
  color: #64748b;
  font-weight: 500;
  transition: .25s;
}

.nav-links a:hover,
.nav-links .router-link-active {
  color: #4f46e5;
}

.nav-user {
  display: flex;
  align-items: center;
  gap: 20px;
}

.nav-user span {
  color: #475569;
  font-weight: 500;
}

.nav-user button {
  background: #ef4444;
  color: white;
  border: none;
  padding: 10px 18px;
  border-radius: 10px;
  cursor: pointer;
  transition: .25s;
}

.nav-user button:hover {
  background: #dc2626;
}

/* ================= CONTENU ================= */

.content {
  max-width: 1100px;
  margin: 40px auto;
  padding: 0 20px;
}

h2 {
  color: #1e293b;
  font-size: 30px;
  margin-bottom: 30px;
}

/* ================= FORMULAIRE ================= */

.create-card {
  background: white;
  border-radius: 18px;
  padding: 28px;
  margin-bottom: 35px;
  box-shadow: 0 10px 30px rgba(0,0,0,.06);
}

.create-card h3 {
  margin-bottom: 22px;
  color: #4f46e5;
}

.form-row {
  display: flex;
  gap: 18px;
  align-items: end;
  flex-wrap: wrap;
}

.field {
  flex: 1;
  min-width: 220px;
}

.field label {
  display: block;
  margin-bottom: 8px;
  color: #475569;
  font-weight: 600;
}

.field input {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid #d1d5db;
  border-radius: 12px;
  background: #f9fafb;
  font-size: 15px;
  transition: .25s;
  box-sizing: border-box;
}

.field input:focus {
  outline: none;
  border-color: #4f46e5;
  background: white;
  box-shadow: 0 0 0 4px rgba(79,70,229,.12);
}

.form-row button {
  height: 48px;
  padding: 0 28px;
  border: none;
  border-radius: 12px;
  background: #4f46e5;
  color: white;
  font-weight: 600;
  cursor: pointer;
  transition: .25s;
}

.form-row button:hover:not(:disabled) {
  background: #4338ca;
}

.form-row button:disabled {
  opacity: .5;
  cursor: not-allowed;
}

/* ================= PLAYLISTS ================= */

.playlists-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pl-card {
  display: flex;
  align-items: center;
  gap: 18px;
  background: white;
  padding: 18px 22px;
  border-radius: 16px;
  box-shadow: 0 5px 15px rgba(0,0,0,.05);
  transition: .25s;
  cursor: pointer;
}

.pl-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 30px rgba(79,70,229,.15);
}

.pl-icon {
  width: 55px;
  height: 55px;
  border-radius: 50%;
  background: #4f46e5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  color: white;
}

.pl-info {
  flex: 1;
}

.pl-name {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

.pl-meta {
  margin-top: 6px;
  color: #64748b;
}

.btn-delete {
  width: 42px;
  height: 42px;
  border: none;
  border-radius: 10px;
  background: #fee2e2;
  color: #dc2626;
  cursor: pointer;
  transition: .25s;
  font-size: 18px;
}

.btn-delete:hover {
  background: #dc2626;
  color: white;
}

.empty {
  text-align: center;
  padding: 80px;
  color: #64748b;
  font-size: 18px;
}
</style>