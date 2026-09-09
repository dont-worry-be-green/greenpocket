import { useAuthStore } from '@/stores/auth'

const AUTH_PATHS = ['/onboarding/login', '/onboarding/signup']

export async function onboardingGuard(to) {
  const auth = useAuthStore()
  await auth.restoreSession()

  const isAuthRoute = AUTH_PATHS.includes(to.path)

  if (!auth.authenticated) return isAuthRoute ? true : '/onboarding/login'
  return isAuthRoute ? '/analysis/eco-link' : true
}
