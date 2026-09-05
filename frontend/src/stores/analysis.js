import { ref } from 'vue'
import { defineStore } from 'pinia'

import { getBillTargetMonth, getDiagnosis } from '@/api/analysis'

export const useAnalysisStore = defineStore('analysis', () => {
  const diagnosis = ref(null)
  const targetMonth = ref(null)
  const selectedImage = ref(null)
  const billDraft = ref(null)
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

  function selectImage(file) {
    selectedImage.value = file
  }

  function saveBillDraft(draft) {
    billDraft.value = draft
  }

  function confirmBillDraft() {
    if (!billDraft.value) return null

    const items = billDraft.value.items ?? []
    diagnosis.value = {
      empty: false,
      screen: 'AN-07',
      yearMonth: billDraft.value.billingMonth,
      summary: {
        currentTotal: items.reduce((total, item) => total + Number(item.amount), 0),
        hasPreviousYear: false,
        items,
      },
    }
    return diagnosis.value
  }

  return {
    diagnosis,
    targetMonth,
    selectedImage,
    billDraft,
    isLoading,
    error,
    fetchHome,
    selectImage,
    saveBillDraft,
    confirmBillDraft,
  }
})
