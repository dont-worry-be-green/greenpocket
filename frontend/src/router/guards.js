import { useAuthStore } from '@/stores/auth'

const AUTH_PATHS = ['/onboarding/start', '/onboarding/login', '/onboarding/signup']
const PROFILE_PATH = '/onboarding/profile'

export async function onboardingGuard(to) {
  const auth = useAuthStore()
  await auth.restoreSession()

  const isAuthRoute = AUTH_PATHS.includes(to.path)
  const isProfileRoute = to.path === PROFILE_PATH

  if (!auth.authenticated) return isAuthRoute ? true : '/onboarding/start'
  if (!auth.onboardingCompleted) return isProfileRoute ? true : PROFILE_PATH
  return isAuthRoute || isProfileRoute ? '/whatif' : true
}
