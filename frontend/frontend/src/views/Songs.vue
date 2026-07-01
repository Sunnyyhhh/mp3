<template>
  <div class="page">
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

      <div class="create-card">
        <h3> Ajouter une chanson</h3>
        <div class="form-row">
          <div class="field file-field">
            <label>Fichier MP3</label>
            <input type="file" accept=".mp3,audio/mpeg" @change="onFileChange" ref="fileInput" />
          </div>
          <div class="field">
            <label>Titre</label>
            <input v-model="newTitre" placeholder="Titre..." />
          </div>
          <div class="field">
            <label>Artiste</label>
            <input v-model="newArtiste" placeholder="Artiste..." />
          </div>
          <div class="field">
            <label>Genre</label>
            <input v-model="newGenre" placeholder="Genre (optionnel)..." />
          </div>
          <button @click="create" :disabled="!canCreate || uploading">
            {{ uploading ? 'Envoi...' : 'Ajouter' }}
          </button>
        </div>
        <p v-if="uploadError" class="error-msg">{{ uploadError }}</p>
      </div>

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

          <div class="song-info" v-if="editingId !== song.id" @click="play(song)">
            <div class="song-title">{{ song.titre }}</div>
            <div class="song-meta">{{ song.artiste }} • {{ formatDuree(song.duree) }}</div>
          </div>

          <div class="song-info edit-mode" v-else @click.stop>
            <input v-model="editTitre" class="edit-input" placeholder="Titre" @keyup.enter="saveEdit(song.id)" />
            <input v-model="editArtiste" class="edit-input" placeholder="Artiste" @keyup.enter="saveEdit(song.id)" />
            <input v-model="editGenre" class="edit-input" placeholder="Genre" @keyup.enter="saveEdit(song.id)" />
          </div>

          <div class="song-genre" v-if="editingId !== song.id">{{ song.genre || '—' }}</div>

          <div class="song-actions">
            <template v-if="editingId !== song.id">
              <button class="song-genre" @click.stop="startEdit(song)">Modifier</button>
            </template>
            <template v-else>
              <button class="btn-save" @click.stop="saveEdit(song.id)">✔</button>
              <button class="btn-cancel" @click.stop="cancelEdit">✖</button>
            </template>
            <button  class="song-genre" @click.stop="remove(song.id)">Supprimer</button>
          </div>
        </div>

        <div v-if="songs.length === 0" class="empty">
          Aucune chanson disponible
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { getSongs, deleteSong, updateSong, uploadMp3Manuel, streamUrl } from '../services/api'

const router = useRouter()
const auth = useAuthStore()

const songs = ref([])
const currentSong = ref(null)
const currentUrl = ref('')
const audioEl = ref(null)

const fileInput = ref(null)
const selectedFile = ref(null)
const newTitre = ref('')
const newArtiste = ref('')
const newGenre = ref('')
const uploading = ref(false)
const uploadError = ref('')

const canCreate = computed(() =>
  selectedFile.value && newTitre.value.trim() && newArtiste.value.trim()
)

const editingId = ref(null)
const editTitre = ref('')
const editArtiste = ref('')
const editGenre = ref('')

onMounted(refresh)

async function refresh() {
  const res = await getSongs()
  songs.value = res.data
}

function onFileChange(e) {
  selectedFile.value = e.target.files[0] || null
}

async function create() {
  if (!canCreate.value) return
  uploadError.value = ''
  uploading.value = true

  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    formData.append('titre', newTitre.value.trim())
    formData.append('artiste', newArtiste.value.trim())
    formData.append('genre', newGenre.value.trim())

    await uploadMp3Manuel(formData)

    selectedFile.value = null
    if (fileInput.value) fileInput.value.value = ''
    newTitre.value = ''
    newArtiste.value = ''
    newGenre.value = ''

    await refresh()
  } catch (e) {
    uploadError.value = "Erreur lors de l'ajout : " + (e.response?.data?.message || e.message)
  } finally {
    uploading.value = false
  }
}

function startEdit(song) {
  editingId.value = song.id
  editTitre.value = song.titre || ''
  editArtiste.value = song.artiste || ''
  editGenre.value = song.genre || ''
}

function cancelEdit() {
  editingId.value = null
}

async function saveEdit(id) {
  await updateSong(id, {
    titre: editTitre.value.trim(),
    artiste: editArtiste.value.trim(),
    genre: editGenre.value.trim()
  })
  editingId.value = null
  await refresh()

  if (currentSong.value?.id === id) {
    const updated = songs.value.find(s => s.id === id)
    if (updated) currentSong.value = updated
  }
}

async function remove(id) {
  if (!confirm('Supprimer cette chanson ?')) return
  await deleteSong(id)
  songs.value = songs.value.filter(s => s.id !== id)
  if (currentSong.value?.id === id) currentSong.value = null
}

function play(song) {
  currentSong.value = song
  currentUrl.value = streamUrl(song.id)
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

/* ================= FORMULAIRE AJOUT ================= */

.create-card {
  background: white;
  border-radius: 18px;
  padding: 28px;
  margin-bottom: 30px;
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
  min-width: 180px;
}

.file-field {
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

.field input[type="file"] {
  padding: 10px 12px;
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

.error-msg {
  color: #dc2626;
  margin-top: 14px;
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
  flex-shrink: 0;
}

.song-info {
  flex: 1;
  cursor: pointer;
  min-width: 0;
}

.song-info.edit-mode {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  cursor: default;
}

.edit-input {
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  flex: 1;
  min-width: 120px;
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
  flex-shrink: 0;
}

.song-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.btn-edit,
.btn-save,
.btn-cancel,
.btn-delete {
  border: none;
  width: 42px;
  height: 42px;
  border-radius: 10px;
  cursor: pointer;
  transition: .25s;
  font-size: 16px;
}

.btn-edit {
  background: #eef2ff;
  color: #4f46e5;
}
.btn-edit:hover { background: #4f46e5; color: white; }

.btn-save {
  background: #dcfce7;
  color: #16a34a;
}
.btn-save:hover { background: #16a34a; color: white; }

.btn-cancel {
  background: #f1f5f9;
  color: #64748b;
}
.btn-cancel:hover { background: #64748b; color: white; }

.btn-delete {
  background: #fee2e2;
  color: #dc2626;
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