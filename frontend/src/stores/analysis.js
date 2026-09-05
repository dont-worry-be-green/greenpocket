import { ref } from 'vue'
import { defineStore } from 'pinia'

import {
  checkBillDuplicates,
  createBill,
  getBillTargetMonth,
  getDiagnosis,
} from '@/api/analysis'

export const useAnalysisStore = defineStore('analysis', () => {
  const diagnosis = ref(null)
  const targetMonth = ref(null)
  const selectedImage = ref(null)
  const billDraft = ref(null)
  const isLoading = ref(false)
  const isSaving = ref(false)
  const error = ref(null)
  const saveError = ref(null)

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

  async function submitBillDraft(draft = billDraft.value) {
    if (!draft) return null

    isSaving.value = true
    saveError.value = null

    try {
      const duplicateResult = await checkBillDuplicates({
        billingMonth: draft.billingMonth,
        utilityTypes: draft.items.map((item) => item.utilityType),
      })
      const duplicates = duplicateResult.results.filter((item) => item.duplicated)
      if (duplicates.length) {
        const duplicateError = new Error('이미 등록된 고지서 항목이 있어요.')
        duplicateError.code = 'BILL_DUPLICATED'
        duplicateError.details = duplicates
        throw duplicateError
      }

      const savedBill = await createBill(draft)
      diagnosis.value = await getDiagnosis({ month: savedBill.recalculated.diagnosisMonth })
      return savedBill
    } catch (nextError) {
      saveError.value = nextError
      return null
    } finally {
      isSaving.value = false
    }
  }

  return {
    diagnosis,
    targetMonth,
    selectedImage,
    billDraft,
    isLoading,
    isSaving,
    error,
    saveError,
    fetchHome,
    selectImage,
    saveBillDraft,
    submitBillDraft,
  }
})
