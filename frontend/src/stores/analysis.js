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
    const currentTotal = items.reduce((total, item) => total + Number(item.amount), 0)
    const previousAmounts = { ELECTRICITY: 40100, WATER: 8300, GAS: 14200 }
    const previousYearTotal = Object.values(previousAmounts).reduce((total, amount) => total + amount, 0)
    diagnosis.value = {
      empty: false,
      screen: 'AN-07',
      yearMonth: billDraft.value.billingMonth,
      profileSummary: '서울 관악구 · 아파트 20평대',
      summary: {
        currentTotal,
        previousYearTotal,
        diffLastYearTotal: currentTotal - previousYearTotal,
        hasPreviousYear: true,
        items,
      },
      lastYearComparison: {
        available: true,
        totalDiff: currentTotal - previousYearTotal,
        items: items.map((item) => ({
          utilityType: item.utilityType,
          lastYearAmount: previousAmounts[item.utilityType],
          thisYearAmount: item.amount,
          diff: item.amount - previousAmounts[item.utilityType],
        })),
      },
      regionComparison: {
        regionLabel: '서울 관악구',
        tabs: [
          {
            utilityType: 'ELECTRICITY',
            available: true,
            myAmount: 43200,
            regionAvgAmount: 38900,
            diffRegion: 4300,
            series: [
              { yearMonth: '2026-03', mine: 35100, regionAvg: 37200 },
              { yearMonth: '2026-04', mine: 40500, regionAvg: 37800 },
              { yearMonth: '2026-05', mine: 36200, regionAvg: 38100 },
              { yearMonth: '2026-06', mine: 42000, regionAvg: 38400 },
              { yearMonth: '2026-07', mine: 37100, regionAvg: 38600 },
              { yearMonth: '2026-08', mine: 43200, regionAvg: 38900 },
            ],
          },
          { utilityType: 'WATER', available: false, myAmount: 8900 },
          { utilityType: 'GAS', available: false, myAmount: 12400 },
        ],
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
