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

      <!-- Barre de fusion -->
      <div class="merge-bar" v-if="playlists.length > 0">
        <label class="select-all">
          <input
            type="checkbox"
            :checked="allSelected"
            @change="toggleSelectAll"
          />
          Tout sélectionner
        </label>

        <span class="merge-count" v-if="selectedIds.size > 0">
          {{ selectedIds.size }} sélectionnée(s)
        </span>

        <button
          class="btn-merge"
          :disabled="selectedIds.size < 2 || merging"
          @click="merge"
        >
          {{ merging ? 'Fusion en cours...' : '🔀 Fusionner' }}
        </button>
      </div>

      <div class="playlists-grid">
        <div
          v-for="pl in playlists"
          :key="pl.id"
          class="pl-card"
          :class="{ selected: selectedIds.has(pl.id) }"
        >
          <input
            type="checkbox"
            class="pl-checkbox"
            :checked="selectedIds.has(pl.id)"
            @click.stop="toggleSelect(pl.id)"
          />

          <div class="pl-icon" @click="goTo(pl.id)">🎶</div>

          <div class="pl-info" @click="goTo(pl.id)">
            <template v-if="editingId !== pl.id">
              <div class="pl-name">{{ pl.nom }}</div>
              <div class="pl-meta">Durée cible : {{ formatDuree(pl.dureeCible) }}</div>
            </template>
            <template v-else>
              <input
                v-model="editNom"
                class="edit-input"
                @click.stop
                @keyup.enter="saveEdit(pl.id)"
              />
              <input
                v-model.number="editDureeMin"
                type="number"
                min="1"
                class="edit-input"
                @click.stop
                @keyup.enter="saveEdit(pl.id)"
              />
            </template>
          </div>

          <div class="pl-actions">
            <template v-if="editingId !== pl.id">
              <button class="btn-edit" @click.stop="startEdit(pl)">✏️</button>
            </template>
            <template v-else>
              <button class="btn-save" @click.stop="saveEdit(pl.id)">✔</button>
              <button class="btn-cancel" @click.stop="cancelEdit">✖</button>
            </template>
            <button class="btn-delete" @click.stop="remove(pl.id)">🗑</button>
          </div>
        </div>

        <div v-if="playlists.length === 0" class="empty">
          Aucune playlist — crée-en une !
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import {
  getPlaylists,
  createPlaylist,
  deletePlaylist,
  fusionnerPlaylists
} from '../services/api'

const router = useRouter()
const auth = useAuthStore()

const playlists = ref([])
const newNom = ref('')
const newDuree = ref(32)

// Sélection pour fusion
const selectedIds = ref(new Set())
const merging = ref(false)

// Édition inline (update nom / durée cible)
const editingId = ref(null)
const editNom = ref('')
const editDureeMin = ref(0)

const allSelected = computed(() =>
  playlists.value.length > 0 && selectedIds.value.size === playlists.value.length
)

onMounted(refresh)

async function refresh() {
  const res = await getPlaylists()
  playlists.value = res.data
  // nettoie la sélection des playlists qui n'existent plus
  selectedIds.value = new Set(
    [...selectedIds.value].filter(id => playlists.value.some(p => p.id === id))
  )
}

async function create() {
  const dureeSecondes = newDuree.value * 60
  await createPlaylist(newNom.value, dureeSecondes, auth.user.id)
  newNom.value = ''
  newDuree.value = 32
  await refresh()
}

async function remove(id) {
  if (!confirm('Supprimer cette playlist ?')) return
  await deletePlaylist(id)
  selectedIds.value.delete(id)
  await refresh()
}

// ── Édition (update) ──────────────────────────────────────
// ⚠️ Pas d'endpoint PUT /playlists/{id} côté backend pour l'instant.
// Dis-moi si tu veux que je l'ajoute (PlaylistController + PlaylistService).
function startEdit(pl) {
  editingId.value = pl.id
  editNom.value = pl.nom
  editDureeMin.value = pl.dureeCible ? Math.floor(pl.dureeCible / 60) : 0
}

function cancelEdit() {
  editingId.value = null
}

async function saveEdit(id) {
  console.warn('Endpoint de mise à jour de playlist non implémenté côté backend')
  editingId.value = null
}

// ── Sélection ──────────────────────────────────────────────
function toggleSelect(id) {
  if (selectedIds.value.has(id)) {
    selectedIds.value.delete(id)
  } else {
    selectedIds.value.add(id)
  }
  // force réactivité (Set n'est pas profondément réactif)
  selectedIds.value = new Set(selectedIds.value)
}

function toggleSelectAll() {
  if (allSelected.value) {
    selectedIds.value = new Set()
  } else {
    selectedIds.value = new Set(playlists.value.map(p => p.id))
  }
}

// ── Fusion ─────────────────────────────────────────────────
async function merge() {
  if (selectedIds.value.size < 2) return
  if (!confirm(`Fusionner ${selectedIds.value.size} playlists en une seule ?`)) return

  merging.value = true
  try {
    const ids = [...selectedIds.value]
    const res = await fusionnerPlaylists(ids, auth.user.id)
    selectedIds.value = new Set()
    await refresh()
    router.push(`/playlists/${res.data.id}`)
  } catch (e) {
    alert('Erreur lors de la fusion : ' + (e.response?.data?.message || e.message))
  } finally {
    merging.value = false
  }
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
  margin-bottom: 25px;
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

/* ================= BARRE DE FUSION ================= */

.merge-bar {
  display: flex;
  align-items: center;
  gap: 20px;
  background: white;
  border-radius: 14px;
  padding: 14px 22px;
  margin-bottom: 20px;
  box-shadow: 0 4px 14px rgba(0,0,0,.05);
}

.select-all {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #475569;
  font-weight: 500;
  cursor: pointer;
}

.merge-count {
  color: #4f46e5;
  font-weight: 600;
  font-size: 14px;
}

.btn-merge {
  margin-left: auto;
  border: none;
  background: #4f46e5;
  color: white;
  padding: 10px 22px;
  border-radius: 10px;
  font-weight: 600;
  cursor: pointer;
  transition: .25s;
}

.btn-merge:hover:not(:disabled) {
  background: #4338ca;
}

.btn-merge:disabled {
  opacity: .4;
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
}

.pl-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 30px rgba(79,70,229,.15);
}

.pl-card.selected {
  border: 2px solid #4f46e5;
  background: #f5f6ff;
}

.pl-checkbox {
  width: 20px;
  height: 20px;
  accent-color: #4f46e5;
  cursor: pointer;
  flex-shrink: 0;
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
  cursor: pointer;
  flex-shrink: 0;
}

.pl-info {
  flex: 1;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pl-name {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

.pl-meta {
  color: #64748b;
}

.edit-input {
  padding: 8px 10px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  max-width: 220px;
}

.pl-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.btn-edit,
.btn-save,
.btn-cancel,
.btn-delete {
  width: 42px;
  height: 42px;
  border: none;
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
.btn-delete:hover { background: #dc2626; color: white; }

.empty {
  text-align: center;
  padding: 80px;
  color: #64748b;
  font-size: 18px;
}
</style>