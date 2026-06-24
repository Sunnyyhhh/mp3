<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="logo">🎵</div>
      <h1>MP3 Manager</h1>
      <p class="subtitle">Connecte-toi pour accéder à ta musique</p>

      <form @submit.prevent="handleLogin">
        <div class="field">
          <label>Email</label>
          <input v-model="email" type="email" placeholder="ton@email.com" required />
        </div>
        <div class="field">
          <label>Mot de passe</label>
          <input v-model="motDePasse" type="password" placeholder="••••••••" required />
        </div>

        <p v-if="error" class="error">{{ error }}</p>

        <button type="submit" :disabled="loading">
          {{ loading ? 'Connexion...' : 'Se connecter' }}
        </button>
      </form>

      <p class="link">
        Pas encore de compte ?
        <RouterLink to="/register">Créer un compte</RouterLink>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const email = ref('')
const motDePasse = ref('')
const error = ref('')
const loading = ref(false)

async function handleLogin() {
  error.value = ''
  loading.value = true
  try {
    await auth.login(email.value, motDePasse.value)
    router.push('/songs')
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f0f1a 0%, #1a1a3e 100%);
}

.auth-card {
  background: #1a1a2e;
  border: 1px solid #2a2a4e;
  border-radius: 16px;
  padding: 48px 40px;
  width: 100%;
  max-width: 420px;
  text-align: center;
}

.logo {
  font-size: 48px;
  margin-bottom: 12px;
}

h1 {
  font-size: 28px;
  font-weight: 700;
  color: #6c63ff;
  margin-bottom: 8px;
}

.subtitle {
  color: #888;
  margin-bottom: 32px;
  font-size: 14px;
}

.field {
  text-align: left;
  margin-bottom: 20px;
}

.field label {
  display: block;
  font-size: 13px;
  color: #aaa;
  margin-bottom: 6px;
}

.field input {
  width: 100%;
  padding: 12px 16px;
  background: #0f0f1a;
  border: 1px solid #2a2a4e;
  border-radius: 8px;
  color: #fff;
  font-size: 15px;
  outline: none;
  transition: border-color 0.2s;
}

.field input:focus {
  border-color: #6c63ff;
}

button {
  width: 100%;
  padding: 14px;
  background: #6c63ff;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 8px;
  transition: background 0.2s;
}

button:hover:not(:disabled) {
  background: #5a52d5;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error {
  color: #ff6b6b;
  font-size: 13px;
  margin-bottom: 12px;
}

.link {
  margin-top: 24px;
  font-size: 14px;
  color: #888;
}

.link a {
  color: #6c63ff;
  text-decoration: none;
  font-weight: 600;
}
</style>