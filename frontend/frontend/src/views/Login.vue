<template>
  <div class="auth-page">
    <div class="auth-card">
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
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #eef4ff 0%, #f8f9fc 100%);
}

.auth-card {
  width: 100%;
  max-width: 430px;
  background: white;
  border-radius: 20px;
  padding: 45px;
  box-shadow: 0 15px 40px rgba(0,0,0,0.08);
}

.logo{
    font-size:60px;
    margin-bottom:15px;
}

h1{
    text-align:center;
    color:#4f46e5;
    margin-bottom:8px;
}

.subtitle{
    text-align:center;
    color:#6b7280;
    margin-bottom:35px;
}

.field{
    margin-bottom:20px;
}

.field label{
    display:block;
    margin-bottom:8px;
    color:#4b5563;
    font-weight:600;
}

.field input{
    width:100%;
    padding:14px 16px;
    border:1px solid #d1d5db;
    border-radius:12px;
    font-size:15px;
    background:#f9fafb;
    transition:.25s;
}

.field input:focus{
    outline:none;
    border-color:#4f46e5;
    background:white;
    box-shadow:0 0 0 4px rgba(79,70,229,.12);
}

button{
    width:100%;
    margin-top:10px;
    padding:15px;
    border:none;
    border-radius:12px;
    background:#4f46e5;
    color:white;
    font-size:16px;
    font-weight:600;
    cursor:pointer;
    transition:.25s;
}

button:hover:not(:disabled){
    background:#4338ca;
    transform:translateY(-2px);
}

button:disabled{
    opacity:.6;
    cursor:not-allowed;
}

.error{
    color:#dc2626;
    font-size:14px;
    margin-bottom:15px;
}

.link{
    text-align:center;
    margin-top:25px;
    color:#6b7280;
}

.link a{
    color:#4f46e5;
    text-decoration:none;
    font-weight:600;
}

.link a:hover{
    text-decoration:underline;
}
</style>