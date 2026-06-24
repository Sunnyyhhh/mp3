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
      <div class="header">
        <button class="btn-back" @click="router.push('/playlists')">← Retour</button>
        <div>
          <h2>{{ playlist?.nom }}</h2>
          <p class="subtitle">
            Durée cible : {{ formatDuree(playlist?.dureeCible) }} •
            {{ morceaux.length }} morceaux •
            Durée totale : {{ formatDuree(totalDuree) }}
          </p>
        </div>
        <button class="btn-zip" @click="downloadZip">⬇ Télécharger ZIP</button>
      </div>

      <!-- Actions -->
      <div class="actions-card">

        <!-- Sélecteur d'artistes -->
        <div class="artist-section">
          <div class="artist-section-header">
            <span class="section-label">🎤 Filtrer par artistes</span>
            <button
              v-if="selectedArtists.length > 0"
              class="btn-clear"
              @click="selectedArtists = []"
            >
              ✕ Effacer la sélection
            </button>
          </div>

          <div v-if="availableArtists.length === 0" class="no-artists">
            Aucun artiste disponible
          </div>
          <div v-else class="artist-chips">
            <span
              v-for="artiste in availableArtists"
              :key="artiste"
              class="chip"
              :class="{ selected: selectedArtists.includes(artiste) }"
              @click="toggleArtist(artiste)"
            >
              {{ artiste }}
            </span>
          </div>
        </div>

        <!-- Boutons de génération -->
        <div class="generate-btns">
          <button class="btn-generate btn-auto" @click="generate">
            🔀 Générer automatiquement
          </button>
          <button
            class="btn-generate btn-artists"
            @click="generateByArtists"
            :disabled="selectedArtists.length === 0"
            :title="selectedArtists.length === 0 ? 'Sélectionne au moins un artiste' : ''"
          >
            🎤 Générer par artistes
            <span v-if="selectedArtists.length > 0" class="badge">{{ selectedArtists.length }}</span>
          </button>
        </div>

      </div>

      <!-- Lecteur -->
      <div v-if="currentMorceau" class="player">
        <div class="player-info">
          <div class="player-icon">🎵</div>
          <div>
            <div class="player-title">{{ currentMorceau.mp3.titre }}</div>
            <div class="player-artist">{{ currentMorceau.mp3.artiste }}</div>
          </div>
        </div>
        <audio
          :src="currentUrl"
          controls
          autoplay
          class="audio-ctrl"
          @ended="playNext"
        ></audio>
      </div>

      <!-- Liste des morceaux -->
      <div class="morceaux-list">
        <div
          v-for="(m) in morceaux"
          :key="m.id"
          class="morceau-card"
          :class="{ active: currentMorceau?.id === m.id }"
        >
          <div class="m-ordre">{{ m.ordre }}</div>
          <div class="m-icon" @click="play(m)">
            {{ currentMorceau?.id === m.id ? '⏸' : '▶' }}
          </div>
          <div class="m-info" @click="play(m)">
            <div class="m-title">{{ m.mp3.titre }}</div>
            <div class="m-meta">{{ m.mp3.artiste }} • {{ formatDuree(m.mp3.duree) }}</div>
          </div>

          <select
            class="select-replace"
            @change="replace(m.id, $event.target.value)"
            :value="m.mp3.id"
          >
            <option
              v-for="s in allSongs"
              :key="s.id"
              :value="s.id"
            >{{ s.artiste }} - {{ s.titre }}</option>
          </select>
        </div>

        <div v-if="morceaux.length === 0" class="empty">
          Sélectionne des artistes puis clique sur "Générer par artistes",<br>
          ou clique sur "Générer automatiquement"
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import {
  getPlaylist,
  getPlaylistSongs,
  generatePlaylist,
  generatePlaylistByArtists,
  replaceSong,
  getSongs,
  streamUrl,
  downloadZip as downloadZipApi
} from '../services/api'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const id = route.params.id
const playlist = ref(null)
const morceaux = ref([])
const allSongs = ref([])
const currentMorceau = ref(null)
const currentUrl = ref('')
const selectedArtists = ref([])

const totalDuree = computed(() =>
  morceaux.value.reduce((acc, m) => acc + (m.mp3.duree || 0), 0)
)

const availableArtists = computed(() =>
  [...new Set(allSongs.value.map(s => s.artiste).filter(Boolean))].sort()
)

onMounted(async () => {
  const [plRes, mRes, sRes] = await Promise.all([
    getPlaylist(id),
    getPlaylistSongs(id),
    getSongs()
  ])
  playlist.value = plRes.data
  morceaux.value = mRes.data
  allSongs.value = sRes.data
})

function toggleArtist(artiste) {
  const idx = selectedArtists.value.indexOf(artiste)
  if (idx === -1) selectedArtists.value.push(artiste)
  else selectedArtists.value.splice(idx, 1)
}

function play(m) {
  currentMorceau.value = m
  currentUrl.value = streamUrl(m.mp3.id)
}

function playNext() {
  const idx = morceaux.value.findIndex(m => m.id === currentMorceau.value?.id)
  if (idx < morceaux.value.length - 1) {
    play(morceaux.value[idx + 1])
  }
}

async function generate() {
  const res = await generatePlaylist(id, playlist.value.dureeCible)
  morceaux.value = res.data
}

async function generateByArtists() {
  const res = await generatePlaylistByArtists(id, selectedArtists.value)
  morceaux.value = res.data
}

async function replace(playlistMp3Id, nouveauMp3Id) {
  await replaceSong(playlistMp3Id, nouveauMp3Id)
  const res = await getPlaylistSongs(id)
  morceaux.value = res.data
}

async function downloadZip() {
  const res = await downloadZipApi(id)
  const url = URL.createObjectURL(new Blob([res.data]))
  const a = document.createElement('a')
  a.href = url
  a.download = `playlist_${playlist.value.nom}.zip`
  a.click()
  URL.revokeObjectURL(url)
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

.header {
  display: flex; align-items: center; gap: 20px;
  margin-bottom: 24px; flex-wrap: wrap;
}
.btn-back {
  background: transparent; border: 1px solid #2a2a4e; color: #aaa;
  padding: 8px 16px; border-radius: 8px; cursor: pointer; font-size: 14px; white-space: nowrap;
}
.btn-back:hover { border-color: #6c63ff; color: #6c63ff; }
h2 { font-size: 24px; font-weight: 700; margin: 0; }
.subtitle { font-size: 13px; color: #888; margin-top: 4px; }
.btn-zip {
  margin-left: auto; padding: 10px 20px; background: #1a1a2e;
  border: 1px solid #6c63ff; color: #6c63ff; border-radius: 8px;
  cursor: pointer; font-size: 14px; font-weight: 600; white-space: nowrap;
}
.btn-zip:hover { background: #6c63ff; color: white; }

/* Actions card */
.actions-card {
  background: #1a1a2e; border: 1px solid #2a2a4e;
  border-radius: 12px; padding: 20px 24px; margin-bottom: 24px;
}

.artist-section { margin-bottom: 20px; }
.artist-section-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 12px;
}
.section-label { font-size: 13px; color: #aaa; font-weight: 500; }
.btn-clear {
  background: transparent; border: none; color: #ff6b6b;
  font-size: 12px; cursor: pointer; padding: 4px 8px;
}
.btn-clear:hover { text-decoration: underline; }

.no-artists { color: #555; font-size: 13px; }

.artist-chips { display: flex; flex-wrap: wrap; gap: 8px; }
.chip {
  padding: 6px 14px; border-radius: 20px; font-size: 13px; cursor: pointer;
  border: 1px solid #2a2a4e; color: #aaa; background: #0f0f1a; transition: all 0.2s;
  user-select: none;
}
.chip:hover { border-color: #6c63ff; color: #6c63ff; }
.chip.selected { background: #6c63ff; color: white; border-color: #6c63ff; }

.generate-btns { display: flex; gap: 12px; flex-wrap: wrap; }
.btn-generate {
  padding: 10px 22px; color: white; border: none;
  border-radius: 8px; font-size: 14px; font-weight: 600; cursor: pointer;
  display: flex; align-items: center; gap: 8px; transition: background 0.2s;
}
.btn-auto { background: #6c63ff; }
.btn-auto:hover { background: #5a52d5; }
.btn-artists { background: #2a2a4e; }
.btn-artists:hover:not(:disabled) { background: #3d3d6e; }
.btn-artists:disabled { opacity: 0.4; cursor: not-allowed; }

.badge {
  background: #6c63ff; color: white; font-size: 11px;
  padding: 2px 8px; border-radius: 20px; font-weight: 700;
}

/* Lecteur */
.player {
  background: #1a1a2e; border: 1px solid #6c63ff; border-radius: 12px;
  padding: 20px 24px; margin-bottom: 24px; display: flex;
  align-items: center; gap: 24px; flex-wrap: wrap;
}
.player-info { display: flex; align-items: center; gap: 16px; flex: 1; }
.player-icon { font-size: 32px; }
.player-title { font-size: 16px; font-weight: 600; }
.player-artist { font-size: 13px; color: #aaa; margin-top: 4px; }
.audio-ctrl { flex: 2; min-width: 200px; }

/* Morceaux */
.morceaux-list { display: flex; flex-direction: column; gap: 8px; }
.morceau-card {
  display: flex; align-items: center; gap: 12px;
  background: #1a1a2e; border: 1px solid #2a2a4e; border-radius: 10px;
  padding: 12px 16px; transition: all 0.2s;
}
.morceau-card:hover { border-color: #6c63ff; }
.morceau-card.active { border-color: #6c63ff; background: #1f1f3a; }

.m-ordre { width: 24px; text-align: center; color: #555; font-size: 13px; }
.m-icon {
  width: 34px; height: 34px; background: #6c63ff; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; cursor: pointer; flex-shrink: 0;
}
.m-info { flex: 1; cursor: pointer; }
.m-title { font-size: 14px; font-weight: 500; }
.m-meta { font-size: 12px; color: #888; margin-top: 3px; }

.select-replace {
  background: #0f0f1a; border: 1px solid #2a2a4e; color: #aaa;
  padding: 6px 10px; border-radius: 6px; font-size: 12px; cursor: pointer;
  max-width: 200px;
}
.select-replace:focus { border-color: #6c63ff; outline: none; }

.empty {
  text-align: center; color: #555; padding: 48px;
  font-size: 15px; line-height: 1.8;
}
</style>