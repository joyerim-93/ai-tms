<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const loginId = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function submit() {
  error.value = ''
  loading.value = true
  try {
    await auth.login(loginId.value, password.value)
    const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/') ? route.query.redirect : '/'
    router.replace(redirect)
  } catch (e) {
    error.value = e.message
    password.value = ''
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <form class="card login-card" @submit.prevent="submit">
      <div class="brand">AI-TMS</div>
      <p class="muted sub">테스트공정관리 포탈</p>

      <label class="label required">아이디</label>
      <input v-model="loginId" class="input" autocomplete="username" autofocus required />

      <label class="label required field">비밀번호</label>
      <input v-model="password" type="password" class="input" autocomplete="current-password" required />

      <p v-if="error" class="error-text">{{ error }}</p>
      <button class="btn btn-primary submit" :disabled="loading || !loginId || !password">
        {{ loading ? '로그인 중…' : '로그인' }}
      </button>
      <p class="muted alt">계정이 없으신가요? <RouterLink :to="{ path: '/register', query: route.query }">회원가입</RouterLink></p>
    </form>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: var(--surface-page);
}
.login-card {
  width: 360px;
  padding: var(--space-6);
}
.brand {
  color: var(--text-primary);
  font-size: var(--font-size-xl);
  font-weight: 800;
  letter-spacing: -0.3px;
}
.sub {
  margin: var(--space-1) 0 var(--space-5);
  font-size: var(--font-size-sm);
}
.field {
  margin-top: var(--space-3);
}
.submit {
  width: 100%;
  margin-top: var(--space-4);
}
.alt {
  margin: var(--space-4) 0 0;
  font-size: var(--font-size-sm);
  text-align: center;
}
.alt a {
  color: var(--accent);
  font-weight: 600;
}
</style>
