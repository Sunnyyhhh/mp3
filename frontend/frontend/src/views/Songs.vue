<template>
  <div class="page">
    <!-- Navbar -->
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
.page { min-height: 100vh; background: #0f0f1a; }

.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 32px;
  background: #1a1a2e;
  border-bottom: 1px solid #2a2a4e;
}

.nav-logo { font-size: 20px; font-weight: 700; color: #6c63ff; }

.nav-links { display: flex; gap: 24px; }
.nav-links a { color: #aaa; text-decoration: none; font-size: 15px; transition: color 0.2s; }
.nav-links a:hover, .nav-links a.router-link-active { color: #6c63ff; }

.nav-user { display: flex; align-items: center; gap: 16px; }
.nav-user span { color: #aaa; font-size: 14px; }
.nav-user button {
  padding: 8px 16px;
  background: transparent;
  border: 1px solid #ff6b6b;
  color: #ff6b6b;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}
.nav-user button:hover { background: #ff6b6b; color: white; }

.content { padding: 32px; max-width: 900px; margin: 0 auto; }

h2 { font-size: 24px; font-weight: 700; margin-bottom: 24px; display: flex; align-items: center; gap: 12px; }

.badge {
  background: #6c63ff;
  color: white;
  font-size: 13px;
  padding: 2px 10px;
  border-radius: 20px;
}

/* Lecteur */
.player {
  background: #1a1a2e;
  border: 1px solid #6c63ff;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}

.player-info { display: flex; align-items: center; gap: 16px; flex: 1; }
.player-icon { font-size: 32px; }
.player-title { font-size: 16px; font-weight: 600; }
.player-artist { font-size: 13px; color: #aaa; margin-top: 4px; }
.audio-ctrl { flex: 2; min-width: 200px; }

/* Liste */
.songs-list { display: flex; flex-direction: column; gap: 8px; }

.song-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #1a1a2e;
  border: 1px solid #2a2a4e;
  border-radius: 10px;
  padding: 14px 20px;
  transition: all 0.2s;
  cursor: pointer;
}

.song-card:hover { border-color: #6c63ff; background: #1f1f3a; }
.song-card.active { border-color: #6c63ff; background: #1f1f3a; }

.song-icon {
  width: 36px;
  height: 36px;
  background: #6c63ff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  flex-shrink: 0;
}

.song-info { flex: 1; }
.song-title { font-size: 15px; font-weight: 500; }
.song-meta { font-size: 12px; color: #888; margin-top: 3px; }
.song-genre { font-size: 12px; color: #6c63ff; min-width: 80px; text-align: center; }

.btn-delete {
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 16px;
  opacity: 0.5;
  transition: opacity 0.2s;
  padding: 4px 8px;
}
.btn-delete:hover { opacity: 1; }

.empty { text-align: center; color: #555; padding: 48px; font-size: 16px; }
</style>