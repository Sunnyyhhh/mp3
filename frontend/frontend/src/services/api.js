import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8080'
})

// ==============================
// MP3
// ==============================
export const getSongs = () => api.get('/mp3')
export const deleteSong = (id) => api.delete(`/mp3/${id}`)
export const streamUrl = (id) => `http://localhost:8080/mp3/${id}/stream`

// ==============================
// USERS
// ==============================
export const register = (data) => api.post('/users', data)
export const getUsers = () => api.get('/users')

// ==============================
// PLAYLISTS
// ==============================
export const getPlaylists = () => api.get('/playlists')
export const getPlaylist = (id) => api.get(`/playlists/${id}`)
export const createPlaylist = (nom, dureeCible, utilisateurId) =>
  api.post(`/playlists?nom=${nom}&dureeCible=${dureeCible}&utilisateurId=${utilisateurId}`)
export const deletePlaylist = (id) => api.delete(`/playlists/${id}`)

// Suggestion sans filtre (respecte durée cible)
export const suggererPlaylist = (id) =>
  api.post(`/playlists/${id}/suggerer`)

// Suggestion avec filtres artistes + genres (respecte durée cible)
export const suggererPlaylistParFiltres = (id, artistes, genres) =>
  api.post(`/playlists/${id}/suggerer-par-filtres`, { artistes, genres })

// Ajouter une chanson manuellement à la suggestion
export const ajouterMorceau = (id, mp3Id) =>
  api.post(`/playlists/${id}/ajouter-morceau?mp3Id=${mp3Id}`)

// Supprimer un morceau de la suggestion
export const supprimerMorceau = (playlistMp3Id) =>
  api.delete(`/playlists/morceaux/${playlistMp3Id}`)

// Confirmer (verrouiller) la playlist
export const confirmerPlaylist = (id) =>
  api.post(`/playlists/${id}/confirmer`)

export const getPlaylistSongs = (id) => api.get(`/playlists/${id}/morceaux`)
export const downloadZip = (id) =>
  api.get(`/playlists/${id}/zip`, { responseType: 'blob' })