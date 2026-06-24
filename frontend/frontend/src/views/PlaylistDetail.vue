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
        <div class="header-right">
          <span class="statut-badge" :class="playlist?.statut === 'CONFIRMEE' ? 'confirmee' : 'brouillon'">
            {{ playlist?.statut === 'CONFIRMEE' ? '✅ Confirmée' : '✏️ Brouillon' }}
          </span>
          <button class="btn-zip" @click="downloadZip">⬇ ZIP</button>
        </div>
      </div>

      <!-- ========== MODE BROUILLON ========== -->
      <template v-if="playlist?.statut === 'BROUILLON'">

        <!-- ETAPE 1 : Filtres -->
        <div class="etape-card">
          <div class="etape-title">① Choisir les filtres <span class="etape-hint">(optionnel)</span></div>

          <div class="filtres-cols">
            <!-- Artistes -->
            <div class="filtre-col">
              <div class="filtre-label">🎤 Artistes</div>
              <label v-for="artiste in availableArtists" :key="artiste" class="checkbox-row">
                <input type="checkbox" :value="artiste" v-model="selectedArtists" />
                <span>{{ artiste }}</span>
              </label>
              <div v-if="availableArtists.length === 0" class="no-items">Aucun artiste</div>
            </div>

            <!-- Genres -->
            <div class="filtre-col">
              <div class="filtre-label">🎵 Genres</div>
              <label v-for="genre in availableGenres" :key="genre" class="checkbox-row">
                <input type="checkbox" :value="genre" v-model="selectedGenres" />
                <span>{{ genre }}</span>
              </label>
              <div v-if="availableGenres.length === 0" class="no-items">Aucun genre</div>
            </div>
          </div>

          <!-- Résumé filtres -->
          <div v-if="selectedArtists.length > 0 || selectedGenres.length > 0" class="filtre-resume">
            <span v-if="selectedArtists.length > 0">🎤 {{ selectedArtists.join(', ') }}</span>
            <span v-if="selectedArtists.length > 0 && selectedGenres.length > 0" class="et">ET</span>
            <span v-if="selectedGenres.length > 0">🎵 {{ selectedGenres.join(', ') }}</span>
          </div>

          <div class="etape-actions">
            <button class="btn-suggerer" @click="suggerer">
              🔍 Suggérer une playlist
            </button>
            <button class="btn-clear-filtres" @click="selectedArtists = []; selectedGenres = []"
              v-if="selectedArtists.length > 0 || selectedGenres.length > 0">
              ✕ Effacer filtres
            </button>
          </div>
        </div>

        <!-- ETAPE 2 : Suggestion + ajout manuel -->
        <div v-if="morceaux.length > 0" class="etape-card">
          <div class="etape-title">② Modifier la suggestion <span class="etape-hint">(ajouter / retirer des chansons)</span></div>

          <!-- Lecteur -->
          <div v-if="currentMorceau" class="player">
            <div class="player-info">
              <div class="player-icon">🎵</div>
              <div>
                <div class="player-title">{{ currentMorceau.mp3.titre }}</div>
                <div class="player-artist">{{ currentMorceau.mp3.artiste }}</div>
              </div>
            </div>
            <audio :src="currentUrl" controls autoplay class="audio-ctrl" @ended="playNext"></audio>
          </div>

          <!-- Liste des morceaux suggérés -->
          <div class="morceaux-list">
            <div v-for="m in morceaux" :key="m.id" class="morceau-card"
              :class="{ active: currentMorceau?.id === m.id }">
              <div class="m-ordre">{{ m.ordre }}</div>
              <div class="m-icon" @click="play(m)">
                {{ currentMorceau?.id === m.id ? '⏸' : '▶' }}
              </div>
              <div class="m-info" @click="play(m)">
                <div class="m-title">{{ m.mp3.titre }}</div>
                <div class="m-meta">{{ m.mp3.artiste }} • {{ m.mp3.genre || '—' }} • {{ formatDuree(m.mp3.duree) }}</div>
              </div>
              <button class="btn-retirer" @click="retirer(m.id)" title="Retirer de la playlist">✕</button>
            </div>
          </div>

          <!-- Ajouter une chanson manuellement -->
          <div class="ajouter-section">
            <div class="ajouter-label">➕ Ajouter une chanson manuellement</div>
            <div class="ajouter-row">
              <select v-model="songToAdd" class="select-song">
                <option value="" disabled>Choisir une chanson...</option>
                <option v-for="s in songsNotInPlaylist" :key="s.id" :value="s.id">
                  {{ s.artiste }} — {{ s.titre }} ({{ formatDuree(s.duree) }})
                </option>
              </select>
              <button class="btn-ajouter" @click="ajouter" :disabled="!songToAdd">
                Ajouter
              </button>
            </div>
          </div>
        </div>

        <!-- ETAPE 3 : Confirmer -->
        <div v-if="morceaux.length > 0" class="etape-card confirmer-card">
          <div class="etape-title">③ Confirmer la playlist</div>
          <p class="confirmer-hint">
            Une fois confirmée, la playlist ne pourra plus être modifiée.<br>
            Tu pourras toujours l'écouter, la télécharger en ZIP ou la supprimer.
          </p>
          <div class="confirmer-recap">
            <span>{{ morceaux.length }} chansons</span>
            <span>•</span>
            <span>{{ formatDuree(totalDuree) }} au total</span>
          </div>
          <button class="btn-confirmer" @click="confirmer">
            ✅ Confirmer la playlist
          </button>
        </div>

      </template>

      <!-- ========== MODE CONFIRMEE ========== -->
      <template v-else>
        <div class="confirmee-banner">
          ✅ Cette playlist est confirmée — elle ne peut plus être modifiée.
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
          <audio :src="currentUrl" controls autoplay class="audio-ctrl" @ended="playNext"></audio>
        </div>

        <!-- Liste morceaux (lecture seule) -->
        <div class="morceaux-list">
          <div v-for="m in morceaux" :key="m.id" class="morceau-card"
            :class="{ active: currentMorceau?.id === m.id }">
            <div class="m-ordre">{{ m.ordre }}</div>
            <div class="m-icon" @click="play(m)">
              {{ currentMorceau?.id === m.id ? '⏸' : '▶' }}
            </div>
            <div class="m-info" @click="play(m)">
              <div class="m-title">{{ m.mp3.titre }}</div>
              <div class="m-meta">{{ m.mp3.artiste }} • {{ m.mp3.genre || '—' }} • {{ formatDuree(m.mp3.duree) }}</div>
            </div>
          </div>
        </div>

        <button class="btn-supprimer" @click="supprimer">🗑 Supprimer cette playlist</button>
      </template>

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import {
  getPlaylist, getPlaylistSongs, getSongs, streamUrl,
  suggererPlaylist, suggererPlaylistParFiltres,
  ajouterMorceau, supprimerMorceau,
  confirmerPlaylist, deletePlaylist,
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
const selectedGenres = ref([])
const songToAdd = ref('')

const totalDuree = computed(() =>
  morceaux.value.reduce((acc, m) => acc + (m.mp3.duree || 0), 0)
)

const availableArtists = computed(() =>
  [...new Set(allSongs.value.map(s => s.artiste).filter(Boolean))].sort()
)

const availableGenres = computed(() =>
  [...new Set(allSongs.value.map(s => s.genre).filter(Boolean))].sort()
)

// Chansons pas encore dans la playlist
const songsNotInPlaylist = computed(() => {
  const ids = morceaux.value.map(m => m.mp3.id)
  return allSongs.value.filter(s => !ids.includes(s.id))
})

async function suggerer() {
  let res
  if (selectedArtists.value.length === 0 && selectedGenres.value.length === 0) {
    res = await suggererPlaylist(id)
  } else {
    res = await suggererPlaylistParFiltres(id, selectedArtists.value, selectedGenres.value)
  }
  morceaux.value = res.data
}

async function ajouter() {
  if (!songToAdd.value) return
  await ajouterMorceau(id, songToAdd.value)
  songToAdd.value = ''
  const res = await getPlaylistSongs(id)
  morceaux.value = res.data
}

async function retirer(playlistMp3Id) {
  await supprimerMorceau(playlistMp3Id)
  morceaux.value = morceaux.value.filter(m => m.id !== playlistMp3Id)
  if (currentMorceau.value?.id === playlistMp3Id) currentMorceau.value = null
}

async function confirmer() {
  if (!confirm('Confirmer la playlist ? Elle ne sera plus modifiable.')) return
  const res = await confirmerPlaylist(id)
  playlist.value = res.data
}

async function supprimer() {
  if (!confirm('Supprimer définitivement cette playlist ?')) return
  await deletePlaylist(id)
  router.push('/playlists')
}

function play(m) {
  currentMorceau.value = m
  currentUrl.value = streamUrl(m.mp3.id)
}

function playNext() {
  const idx = morceaux.value.findIndex(m => m.id === currentMorceau.value?.id)
  if (idx < morceaux.value.length - 1) play(morceaux.value[idx + 1])
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

onMounted(async () => {
  const [plRes, mRes, sRes] = await Promise.all([
    getPlaylist(id), getPlaylistSongs(id), getSongs()
  ])
  playlist.value = plRes.data
  morceaux.value = mRes.data
  allSongs.value = sRes.data
})
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

.header { display: flex; align-items: center; gap: 16px; margin-bottom: 24px; flex-wrap: wrap; }
.btn-back {
  background: transparent; border: 1px solid #2a2a4e; color: #aaa;
  padding: 8px 16px; border-radius: 8px; cursor: pointer; font-size: 14px; white-space: nowrap;
}
.btn-back:hover { border-color: #6c63ff; color: #6c63ff; }
h2 { font-size: 22px; font-weight: 700; margin: 0; }
.subtitle { font-size: 13px; color: #888; margin-top: 4px; }
.header-right { margin-left: auto; display: flex; align-items: center; gap: 12px; }

.statut-badge {
  padding: 5px 14px; border-radius: 20px; font-size: 12px; font-weight: 600;
}
.statut-badge.brouillon { background: #2a2a00; border: 1px solid #aaaa00; color: #eeee00; }
.statut-badge.confirmee { background: #002a00; border: 1px solid #00aa00; color: #00ee00; }

.btn-zip {
  padding: 8px 16px; background: #1a1a2e; border: 1px solid #6c63ff;
  color: #6c63ff; border-radius: 8px; cursor: pointer; font-size: 13px; font-weight: 600;
}
.btn-zip:hover { background: #6c63ff; color: white; }

/* ETAPES */
.etape-card {
  background: #1a1a2e; border: 1px solid #2a2a4e;
  border-radius: 12px; padding: 24px; margin-bottom: 20px;
}
.etape-title {
  font-size: 15px; font-weight: 700; color: #6c63ff; margin-bottom: 18px;
}
.etape-hint { font-size: 12px; color: #555; font-weight: 400; margin-left: 8px; }

/* Filtres checkboxes */
.filtres-cols { display: flex; gap: 40px; flex-wrap: wrap; margin-bottom: 16px; }
.filtre-col { flex: 1; min-width: 180px; }
.filtre-label { font-size: 12px; color: #aaa; font-weight: 600; text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 10px; }

.checkbox-row {
  display: flex; align-items: center; gap: 10px;
  padding: 7px 0; cursor: pointer; font-size: 14px; color: #ccc;
  border-bottom: 1px solid #1f1f3a; transition: color 0.15s;
}
.checkbox-row:hover { color: #fff; }
.checkbox-row input[type="checkbox"] {
  width: 16px; height: 16px; accent-color: #6c63ff; cursor: pointer;
}

.no-items { color: #555; font-size: 13px; }

.filtre-resume {
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
  font-size: 13px; color: #aaa; padding: 10px 12px;
  background: #0f0f1a; border-radius: 8px; margin-bottom: 16px;
}
.et { color: #6c63ff; font-weight: 700; font-size: 11px; padding: 2px 6px; background: #6c63ff22; border-radius: 4px; }

.etape-actions { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.btn-suggerer {
  padding: 10px 24px; background: #6c63ff; color: white; border: none;
  border-radius: 8px; font-size: 14px; font-weight: 600; cursor: pointer;
}
.btn-suggerer:hover { background: #5a52d5; }
.btn-clear-filtres {
  background: transparent; border: none; color: #ff6b6b; font-size: 13px; cursor: pointer;
}
.btn-clear-filtres:hover { text-decoration: underline; }

/* Ajouter manuellement */
.ajouter-section { margin-top: 20px; padding-top: 16px; border-top: 1px solid #2a2a4e; }
.ajouter-label { font-size: 13px; color: #aaa; font-weight: 600; margin-bottom: 10px; }
.ajouter-row { display: flex; gap: 10px; flex-wrap: wrap; }
.select-song {
  flex: 1; min-width: 200px; background: #0f0f1a; border: 1px solid #2a2a4e;
  color: #fff; padding: 10px 14px; border-radius: 8px; font-size: 14px; outline: none;
}
.select-song:focus { border-color: #6c63ff; }
.btn-ajouter {
  padding: 10px 20px; background: #6c63ff; color: white; border: none;
  border-radius: 8px; font-size: 14px; font-weight: 600; cursor: pointer; white-space: nowrap;
}
.btn-ajouter:hover:not(:disabled) { background: #5a52d5; }
.btn-ajouter:disabled { opacity: 0.4; cursor: not-allowed; }

/* Confirmer */
.confirmer-card { border-color: #6c63ff55; }
.confirmer-hint { font-size: 13px; color: #888; line-height: 1.7; margin-bottom: 16px; }
.confirmer-recap {
  display: flex; gap: 12px; align-items: center;
  font-size: 14px; color: #ccc; margin-bottom: 20px;
}
.btn-confirmer {
  padding: 12px 32px; background: #00aa44; color: white; border: none;
  border-radius: 8px; font-size: 15px; font-weight: 700; cursor: pointer;
}
.btn-confirmer:hover { background: #00cc55; }

/* Confirmée */
.confirmee-banner {
  background: #002a00; border: 1px solid #00aa00; color: #00ee00;
  padding: 14px 20px; border-radius: 10px; font-size: 14px;
  font-weight: 600; margin-bottom: 24px;
}

/* Lecteur */
.player {
  background: #0f0f1a; border: 1px solid #6c63ff; border-radius: 12px;
  padding: 16px 20px; margin-bottom: 16px; display: flex; align-items: center; gap: 20px; flex-wrap: wrap;
}
.player-info { display: flex; align-items: center; gap: 14px; flex: 1; }
.player-icon { font-size: 28px; }
.player-title { font-size: 15px; font-weight: 600; }
.player-artist { font-size: 12px; color: #aaa; margin-top: 3px; }
.audio-ctrl { flex: 2; min-width: 200px; }

/* Morceaux */
.morceaux-list { display: flex; flex-direction: column; gap: 6px; margin-bottom: 8px; }
.morceau-card {
  display: flex; align-items: center; gap: 10px;
  background: #0f0f1a; border: 1px solid #2a2a4e; border-radius: 8px;
  padding: 10px 14px; transition: all 0.2s;
}
.morceau-card:hover { border-color: #6c63ff; }
.morceau-card.active { border-color: #6c63ff; background: #1a1a3a; }
.m-ordre { width: 22px; text-align: center; color: #555; font-size: 12px; }
.m-icon {
  width: 30px; height: 30px; background: #6c63ff; border-radius: 50%;
  display: flex; align-items: center; justify-content: center; font-size: 12px; cursor: pointer; flex-shrink: 0;
}
.m-info { flex: 1; cursor: pointer; }
.m-title { font-size: 14px; font-weight: 500; }
.m-meta { font-size: 11px; color: #888; margin-top: 2px; }
.btn-retirer {
  background: transparent; border: none; color: #ff6b6b; cursor: pointer;
  font-size: 14px; padding: 4px 8px; opacity: 0.6; transition: opacity 0.2s;
}
.btn-retirer:hover { opacity: 1; }

.btn-supprimer {
  margin-top: 24px; padding: 10px 24px; background: transparent;
  border: 1px solid #ff6b6b; color: #ff6b6b; border-radius: 8px;
  cursor: pointer; font-size: 14px;
}
.btn-supprimer:hover { background: #ff6b6b; color: white; }
</style>