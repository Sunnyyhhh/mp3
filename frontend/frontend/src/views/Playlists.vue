<template>
  <div class="page">
    <nav class="navbar">
      <div class="nav-logo">🎵 MP3 Manager</div>
      <div class="nav-links">
        <RouterLink to="/songs">Chansons</RouterLink>
        <RouterLink to="/playlists">Playlists</RouterLink>
      </div>
      <div class="nav-user">
        <span>👤 {{ auth.user?.nom }}</span>
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
.page { min-height: 100vh; background: #0f0f1a; color: #fff; }

.navbar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 32px; background: #1a1a2e; border-bottom: 1px solid #2a2a4e;
}
.nav-logo { font-size: 20px; font-weight: 700; color: #6c63ff; }
.nav-links { display: flex; gap: 24px; }
.nav-links a { color: #aaa; text-decoration: none; font-size: 15px; }
.nav-links a:hover, .nav-links a.router-link-active { color: #6c63ff; }
.nav-user { display: flex; align-items: center; gap: 16px; }
.nav-user span { color: #aaa; font-size: 14px; }
.nav-user button {
  padding: 8px 16px; background: transparent; border: 1px solid #ff6b6b;
  color: #ff6b6b; border-radius: 6px; cursor: pointer; font-size: 13px;
}
.nav-user button:hover { background: #ff6b6b; color: white; }

.content { padding: 32px; max-width: 900px; margin: 0 auto; }
h2 { font-size: 24px; font-weight: 700; margin-bottom: 24px; }

.create-card {
  background: #1a1a2e; border: 1px solid #2a2a4e;
  border-radius: 12px; padding: 24px; margin-bottom: 32px;
}
.create-card h3 { font-size: 16px; margin-bottom: 16px; color: #6c63ff; }

.form-row { display: flex; gap: 16px; align-items: flex-end; flex-wrap: wrap; }
.field { flex: 1; min-width: 160px; }
.field label { display: block; font-size: 13px; color: #aaa; margin-bottom: 6px; }
.field input {
  width: 100%; padding: 10px 14px; background: #0f0f1a;
  border: 1px solid #2a2a4e; border-radius: 8px; color: #fff; font-size: 14px; outline: none;
  box-sizing: border-box;
}
.field input:focus { border-color: #6c63ff; }

.form-row > button {
  padding: 10px 24px; background: #6c63ff; color: white; border: none;
  border-radius: 8px; font-size: 15px; font-weight: 600; cursor: pointer; white-space: nowrap;
}
.form-row > button:hover:not(:disabled) { background: #5a52d5; }
.form-row > button:disabled { opacity: 0.4; cursor: not-allowed; }

.playlists-grid { display: flex; flex-direction: column; gap: 10px; }

.pl-card {
  display: flex; align-items: center; gap: 16px;
  background: #1a1a2e; border: 1px solid #2a2a4e; border-radius: 10px;
  padding: 16px 20px; cursor: pointer; transition: all 0.2s;
}
.pl-card:hover { border-color: #6c63ff; background: #1f1f3a; }

.pl-icon { font-size: 28px; }
.pl-info { flex: 1; }
.pl-name { font-size: 16px; font-weight: 600; }
.pl-meta { font-size: 12px; color: #888; margin-top: 4px; }

.btn-delete {
  background: transparent; border: none; cursor: pointer;
  font-size: 16px; opacity: 0.5; transition: opacity 0.2s; padding: 4px 8px;
}
.btn-delete:hover { opacity: 1; }

.empty { text-align: center; color: #555; padding: 48px; font-size: 16px; }
</style>