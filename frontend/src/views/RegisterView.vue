<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

// 회원가입 → 가입 즉시 로그인. 아이디 3~50자(영문·숫자·. _ -), 비밀번호 8자 이상
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const username = ref('')
const displayName = ref('')
const password = ref('')
const passwordConfirm = ref('')
const error = ref('')
const loading = ref(false)

const mismatch = computed(() => passwordConfirm.value !== '' && password.value !== passwordConfirm.value)
const canSubmit = computed(
  () => username.value && displayName.value.trim() && password.value.length >= 8 && password.value === passwordConfirm.value && !loading.value,
)

async function submit() {
  error.value = ''
  loading.value = true
  try {
    await auth.register(username.value.trim(), password.value, displayName.value.trim())
    const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/') ? route.query.redirect : '/'
    router.replace(redirect)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page">
    <form class="card box" @submit.prevent="submit">
      <div class="brand">AI-TMS</div>
      <p class="muted sub">회원가입</p>

      <label class="label required">아이디</label>
      <input v-model="username" class="input" minlength="3" maxlength="50" pattern="[A-Za-z0-9._\-]+" autocomplete="username" autofocus required />
      <p class="muted hint">영문·숫자·. _ - 3~50자</p>

      <label class="label required field">표시 이름</label>
      <input v-model="displayName" class="input" maxlength="50" placeholder="작성자·실행자로 표시됩니다" required />

      <label class="label required field">비밀번호</label>
      <input v-model="password" type="password" class="input" minlength="8" maxlength="64" autocomplete="new-password" required />
      <p class="muted hint">8자 이상</p>

      <label class="label required field">비밀번호 확인</label>
      <input v-model="passwordConfirm" type="password" class="input" autocomplete="new-password" required />
      <p v-if="mismatch" class="error-text">비밀번호가 일치하지 않습니다.</p>

      <p v-if="error" class="error-text">{{ error }}</p>
      <button class="btn btn-primary submit" :disabled="!canSubmit">{{ loading ? '가입 중…' : '가입하고 시작하기' }}</button>
      <p class="muted alt">이미 계정이 있으신가요? <RouterLink :to="{ path: '/login', query: route.query }">로그인</RouterLink></p>
    </form>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: var(--surface-page);
}
.box {
  width: 380px;
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
.hint {
  margin: var(--space-1) 0 0;
  font-size: var(--font-size-xs);
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
