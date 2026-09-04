import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import {
  getPocketHome,
  getPocketManagement,
  getPocketTransactions,
  getWithdrawalAccounts,
  getWithdrawals,
  requestWithdrawal,
} from '@/api/pocket'
import { newIdempotencyKey } from '@/api/client'

export const usePocketStore = defineStore('pocket', () => {
  const home = ref(null)
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
  }

  async function fetchTransactions() {
    const data = await run(() => getPocketTransactions({ direction: 'CREDIT', page: 0, size: 20 }))
    if (data) transactions.value = data
  }

  async function fetchManagement() {
    const data = await run(getPocketManagement)
    if (data) {
      management.value = data
      accounts.value = data.accounts ?? []
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
    fetchHome,
    fetchTransactions,
    fetchManagement,
    fetchWithdrawalAccounts,
    fetchWithdrawals,
    withdraw,
  }
})
