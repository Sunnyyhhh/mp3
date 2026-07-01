import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8080'
})

// ==============================
// MP3
// ==============================
export const getSongs    = ()      => api.get('/mp3')
export const deleteSong  = (id)    => api.delete(`/mp3/${id}`)
export const streamUrl   = (id)    => `http://localhost:8080/mp3/${id}/stream`
export const updateSong  = (id, data) => api.put(`/mp3/${id}`, null, { params: data })

/**
 * Upload manuel depuis le formulaire web.
 * Utilise les champs titre/artiste/genre du formulaire, ignore les métadonnées du fichier.
 */
export const uploadMp3Manuel = (formData) =>
  api.post('/mp3/upload-manuel', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })

// ==============================
// USERS
// ==============================
export const register = (data) => api.post('/users', data)
export const getUsers = ()     => api.get('/users')

// ==============================
// PLAYLISTS
// ==============================
export const getPlaylists  = ()    => api.get('/playlists')
export const getPlaylist   = (id)  => api.get(`/playlists/${id}`)
export const deletePlaylist = (id) => api.delete(`/playlists/${id}`)

export const createPlaylist = (nom, dureeCible, utilisateurId) =>
  api.post(`/playlists?nom=${nom}&dureeCible=${dureeCible}&utilisateurId=${utilisateurId}`)

export const suggererPlaylist = (id) =>
  api.post(`/playlists/${id}/suggerer`)

export const suggererPlaylistParFiltres = (id, artistes, genres) =>
  api.post(`/playlists/${id}/suggerer-par-filtres`, { artistes, genres })

export const ajouterMorceau = (id, mp3Id) =>
  api.post(`/playlists/${id}/ajouter-morceau?mp3Id=${mp3Id}`)

export const supprimerMorceau = (playlistMp3Id) =>
  api.delete(`/playlists/morceaux/${playlistMp3Id}`)

export const confirmerPlaylist = (id) =>
  api.post(`/playlists/${id}/confirmer`)

export const getPlaylistSongs = (id) =>
  api.get(`/playlists/${id}/morceaux`)

export const downloadZip = (id) =>
  api.get(`/playlists/${id}/zip`, { responseType: 'blob' })

/**
 * @param {number[]} ids - IDs des playlists à fusionner (min 2)
 * @param {number} utilisateurId
 */
export const fusionnerPlaylists = (ids, utilisateurId) =>
  api.post('/playlists/fusionner', { ids, utilisateurId })