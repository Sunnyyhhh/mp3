import { defineStore } from 'pinia'
import { getUsers, register } from '../services/api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: JSON.parse(localStorage.getItem('user')) || null
  }),

  actions: {
    async login(email, motDePasse) {
      const response = await getUsers()
      const users = response.data
      const found = users.find(
        u => u.email === email && u.motDePasse === motDePasse
      )
      if (!found) throw new Error('Email ou mot de passe incorrect')
      this.user = found
      localStorage.setItem('user', JSON.stringify(found))
    },

    async register(nom, email, motDePasse) {
      const response = await register({ nom, email, motDePasse })
      this.user = response.data
      localStorage.setItem('user', JSON.stringify(response.data))
    },

    logout() {
      this.user = null
      localStorage.removeItem('user')
    }
  }
})