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
export const generatePlaylist = (id, dureeCible) =>
  api.post(`/playlists/${id}/generer?dureeCible=${dureeCible}`)
export const getPlaylistSongs = (id) => api.get(`/playlists/${id}/morceaux`)
export const replaceSong = (playlistMp3Id, nouveauMp3Id) =>
  api.put(`/playlists/morceaux/${playlistMp3Id}/remplacer?nouveauMp3Id=${nouveauMp3Id}`)
export const downloadZip = (id) =>
  api.get(`/playlists/${id}/zip`, { responseType: 'blob' })
export const generatePlaylistByArtists = (id, artistes) =>
  api.post(`/playlists/${id}/generer-par-artistes`, artistes)