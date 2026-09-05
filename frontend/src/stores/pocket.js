import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import {
  completeMileageConversion,
  getConvertibleMileage,
  getPocketBalance,
  getPocketHome,
  getPocketManagement,
  getPocketTransactions,
  getWithdrawalAccounts,
  getWithdrawals,
  requestWithdrawal,
  startMileageConversion,
} from '@/api/pocket'
import { newIdempotencyKey } from '@/api/client'

export const usePocketStore = defineStore('pocket', () => {
  const home = ref(null)
  const balance = ref(null)
  const convertibleMileage = ref(null)
  const transactions = ref(null)
  const management = ref(null)
  const accounts = ref([])
  const withdrawals = ref(null)
  const withdrawalResult = ref(null)
  const isLoading = ref(false)
  const error = ref(null)
  const accountsLoading = ref(false)
  const accountsLoaded = ref(false)
  const accountsError = ref(null)
  const withdrawalsLoading = ref(false)
  const withdrawalsError = ref(null)
  const withdrawalError = ref(null)
  const withdrawalKey = ref(null)
  const conversionError = ref(null)
  const conversionLoading = ref(false)
  const pendingConversion = ref(null)

  const defaultAccount = computed(
    () => accounts.value.find((account) => account.isDefault) ?? accounts.value[0] ?? null,
  )

  async function run(task) {
    isLoading.value = true
    error.value = null
    try {
      return await task()
    } catch (nextError) {
      error.value = nextError
      return null
    } finally {
      isLoading.value = false
    }
  }

  async function fetchHome() {
    const data = await run(getPocketHome)
    if (data) home.value = data
    return data
  }

  async function fetchBalance() {
    const data = await run(getPocketBalance)
    if (data) balance.value = data
    return data
  }

  async function fetchConvertibleMileage() {
    const data = await run(getConvertibleMileage)
    if (data) convertibleMileage.value = data
    return data
  }

  async function fetchTransactions(direction = null) {
    const params = { page: 0, size: 20 }
    if (direction) params.direction = direction
    const data = await run(() => getPocketTransactions(params))
    if (data) transactions.value = data
    return data
  }

  async function fetchManagement() {
    const data = await run(getPocketManagement)
    if (data) {
      management.value = data
      accounts.value = data.accounts ?? []
      accountsLoaded.value = true
    }
    return data
  }

  async function startConversion(roundId) {
    conversionLoading.value = true
    conversionError.value = null
    try {
      const started = await startMileageConversion({ roundId, agreed: true })
      pendingConversion.value = started
      return started
    } catch (nextError) {
      conversionError.value = nextError
      return null
    } finally {
      conversionLoading.value = false
    }
  }

  async function completeConversion() {
    if (!pendingConversion.value) return null
    conversionLoading.value = true
    conversionError.value = null
    try {
      const result = await completeMileageConversion(
        pendingConversion.value.conversionId,
        newIdempotencyKey(),
      )
      pendingConversion.value = null
      await fetchHome()
      return result
    } catch (nextError) {
      conversionError.value = nextError
      return null
    } finally {
      conversionLoading.value = false
    }
  }

  async function fetchWithdrawalAccounts() {
    accountsLoading.value = true
    accountsError.value = null
    try {
      const data = await getWithdrawalAccounts()
      accounts.value = data.accounts ?? []
      accountsLoaded.value = true
      return accounts.value
    } catch (nextError) {
      accountsError.value = nextError
      return null
    } finally {
      accountsLoading.value = false
    }
  }

  async function fetchWithdrawals(page = 0, size = 20) {
    withdrawalsLoading.value = true
    withdrawalsError.value = null
    try {
      const data = await getWithdrawals({ page, size })
      withdrawals.value = data
      return data
    } catch (nextError) {
      withdrawalsError.value = nextError
      return null
    } finally {
      withdrawalsLoading.value = false
    }
  }

  async function withdraw(amount, accountId) {
    withdrawalError.value = null
    if (!withdrawalKey.value) withdrawalKey.value = newIdempotencyKey()
    try {
      const result = await requestWithdrawal({ amount, accountId }, withdrawalKey.value)
      if (result.transactionStatus !== 'COMPLETED') {
        withdrawalError.value = new Error(
          '출금 처리가 완료되지 않았어요. 잠시 후 다시 확인해 주세요.',
        )
        return null
      }
      withdrawalResult.value = result
      withdrawalKey.value = null
      return result
    } catch (nextError) {
      withdrawalError.value = nextError
      return null
    }
  }

  return {
    home,
    balance,
    convertibleMileage,
    transactions,
    management,
    accounts,
    withdrawals,
    defaultAccount,
    withdrawalResult,
    isLoading,
    error,
    accountsLoading,
    accountsLoaded,
    accountsError,
    withdrawalsLoading,
    withdrawalsError,
    withdrawalError,
    conversionError,
    conversionLoading,
    pendingConversion,
    fetchHome,
    fetchBalance,
    fetchConvertibleMileage,
    fetchTransactions,
    fetchManagement,
    fetchWithdrawalAccounts,
    fetchWithdrawals,
    withdraw,
    startConversion,
    completeConversion,
  }
})
