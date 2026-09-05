import { ref } from 'vue'
import { defineStore } from 'pinia'

import {
  checkBillDuplicates,
  createBill,
  getBillOcrResult,
  getBillTargetMonth,
  getDiagnosis,
  startBillOcr,
} from '@/api/analysis'

const OCR_TERMINAL_STATUSES = new Set(['SUCCEEDED', 'PARTIAL', 'FAILED', 'TIMEOUT'])
const DEFAULT_POLL_AFTER_MS = 1000

export const useAnalysisStore = defineStore('analysis', () => {
  const diagnosis = ref(null)
  const targetMonth = ref(null)
  const selectedImage = ref(null)
  const billDraft = ref(null)
  const ocrResult = ref(null)
  const ocrProgress = ref(0)
  const isAnalyzing = ref(false)
  const ocrError = ref(null)
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
    ocrResult.value = null
    ocrProgress.value = 0
    ocrError.value = null
  }

  function wait(ms) {
    return new Promise((resolve) => window.setTimeout(resolve, ms))
  }

  async function analyzeSelectedImage(billingMonthHint) {
    if (!selectedImage.value) {
      ocrError.value = new Error('분석할 고지서 사진을 다시 선택해 주세요.')
      return null
    }

    isAnalyzing.value = true
    ocrError.value = null
    ocrResult.value = null
    ocrProgress.value = 0

    try {
      const started = await startBillOcr({
        image: selectedImage.value,
        billingMonthHint,
      })
      let current = started
      const pollAfterMs = started.pollAfterMs ?? DEFAULT_POLL_AFTER_MS

      ocrProgress.value = started.progress ?? 0

      while (!OCR_TERMINAL_STATUSES.has(current.status)) {
        await wait(pollAfterMs)
        current = await getBillOcrResult(started.jobId)
        ocrProgress.value = current.progress ?? ocrProgress.value
      }

      ocrResult.value = current
      if (current.status === 'FAILED' || current.status === 'TIMEOUT') {
        const resultError = new Error(current.message ?? '고지서를 읽지 못했어요.')
        resultError.code = current.errorCode ?? current.status
        throw resultError
      }

      return current
    } catch (nextError) {
      ocrError.value = nextError
      return null
    } finally {
      isAnalyzing.value = false
    }
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
    ocrResult,
    ocrProgress,
    isAnalyzing,
    ocrError,
    isLoading,
    isSaving,
    error,
    saveError,
    fetchHome,
    selectImage,
    analyzeSelectedImage,
    saveBillDraft,
    submitBillDraft,
  }
})
