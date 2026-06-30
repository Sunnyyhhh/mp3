<template>
  <div class="page">
    <!-- Navbar -->
    <nav class="navbar">
      <div class="nav-logo"> MP3 Manager</div>
      <div class="nav-links">
        <RouterLink to="/songs">Chansons</RouterLink>
        <RouterLink to="/playlists">Playlists</RouterLink>
      </div>
      <div class="nav-user">
        <span> {{ auth.user?.nom }}</span>
        <button @click="logout">Déconnexion</button>
      </div>
    </nav>

    <div class="content">
      <h2>Mes Chansons <span class="badge">{{ songs.length }}</span></h2>

      <!-- Lecteur audio -->
      <div v-if="currentSong" class="player">
        <div class="player-info">
          <div class="player-icon">🎵</div>
          <div>
            <div class="player-title">{{ currentSong.titre }}</div>
            <div class="player-artist">{{ currentSong.artiste }}</div>
          </div>
        </div>
        <audio
          ref="audioEl"
          :src="currentUrl"
          controls
          autoplay
          class="audio-ctrl"
        ></audio>
      </div>

      <!-- Liste des chansons -->
      <div class="songs-list">
        <div
          v-for="song in songs"
          :key="song.id"
          class="song-card"
          :class="{ active: currentSong?.id === song.id }"
        >
          <div class="song-icon" @click="play(song)">
            {{ currentSong?.id === song.id ? '⏸' : '▶' }}
          </div>
          <div class="song-info" @click="play(song)">
            <div class="song-title">{{ song.titre }}</div>
            <div class="song-meta">{{ song.artiste }} • {{ formatDuree(song.duree) }}</div>
          </div>
          <div class="song-genre">{{ song.genre || '—' }}</div>
          <button class="btn-delete" @click="remove(song.id)">🗑</button>
        </div>

        <div v-if="songs.length === 0" class="empty">
          Aucune chanson disponible
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { getSongs, deleteSong, streamUrl } from '../services/api'

const router = useRouter()
const auth = useAuthStore()

const songs = ref([])
const currentSong = ref(null)
const currentUrl = ref('')
const audioEl = ref(null)

onMounted(async () => {
  const res = await getSongs()
  songs.value = res.data
})

function play(song) {
  currentSong.value = song
  currentUrl.value = streamUrl(song.id)
}

async function remove(id) {
  if (!confirm('Supprimer cette chanson ?')) return
  await deleteSong(id)
  songs.value = songs.value.filter(s => s.id !== id)
  if (currentSong.value?.id === id) currentSong.value = null
}

function formatDuree(sec) {
  if (!sec) return '0:00'
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${m}:${s.toString().padStart(2, '0')}`
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
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 30px;
  color: #1e293b;
  font-size: 30px;
}

.badge {
  background: #4f46e5;
  color: white;
  padding: 5px 12px;
  border-radius: 30px;
  font-size: 14px;
}

/* ================= PLAYER ================= */

.player {
  background: white;
  border-radius: 18px;
  padding: 22px 25px;
  display: flex;
  align-items: center;
  gap: 25px;
  margin-bottom: 35px;
  box-shadow: 0 10px 30px rgba(0,0,0,.06);
}

.player-info {
  display: flex;
  align-items: center;
  gap: 18px;
  flex: 1;
}

.player-icon {
  width: 65px;
  height: 65px;
  border-radius: 50%;
  background: #4f46e5;
  color: white;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 28px;
}

.player-title {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
}

.player-artist {
  color: #64748b;
  margin-top: 5px;
}

.audio-ctrl {
  flex: 2;
}

/* ================= LISTE ================= */

.songs-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.song-card {
  display: flex;
  align-items: center;
  gap: 18px;
  background: white;
  border-radius: 16px;
  padding: 18px 22px;
  box-shadow: 0 4px 14px rgba(0,0,0,.05);
  transition: .25s;
}

.song-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 30px rgba(79,70,229,.15);
}

.song-card.active {
  border: 2px solid #4f46e5;
}

.song-icon {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: #4f46e5;
  color: white;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  font-size: 18px;
}

.song-info {
  flex: 1;
  cursor: pointer;
}

.song-title {
  font-size: 17px;
  font-weight: 600;
  color: #1e293b;
}

.song-meta {
  margin-top: 5px;
  color: #64748b;
  font-size: 14px;
}

.song-genre {
  background: #eef2ff;
  color: #4f46e5;
  padding: 6px 12px;
  border-radius: 25px;
  font-size: 13px;
  font-weight: 600;
}

.btn-delete {
  border: none;
  background: #fee2e2;
  color: #dc2626;
  width: 42px;
  height: 42px;
  border-radius: 10px;
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