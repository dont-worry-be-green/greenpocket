import { ref } from 'vue'
import { defineStore } from 'pinia'

import { getBillTargetMonth, getDiagnosis } from '@/api/analysis'

export const useAnalysisStore = defineStore('analysis', () => {
  const diagnosis = ref(null)
  const targetMonth = ref(null)
  const isLoading = ref(false)
  const error = ref(null)

  async function fetchHome() {
    isLoading.value = true
    error.value = null

    try {
      diagnosis.value = await getDiagnosis()

      if (diagnosis.value?.empty) {
        targetMonth.value = await getBillTargetMonth()
      }

      return diagnosis.value
    } catch (nextError) {
      error.value = nextError
      return null
    } finally {
      isLoading.value = false
    }
  }

  return { diagnosis, targetMonth, isLoading, error, fetchHome }
})
