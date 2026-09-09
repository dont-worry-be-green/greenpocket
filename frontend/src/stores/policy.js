import { ref } from 'vue'
import { defineStore } from 'pinia'

import { getPolicies, getPolicy, getPolicyRecommendations } from '@/api/policy'

export const usePolicyStore = defineStore('policy', () => {
  const list = ref(null)
  const detail = ref(null)
  const loading = ref(false)
  const detailLoading = ref(false)
  const error = ref(null)
  const detailError = ref(null)

  async function fetchList(params = {}, { recommended = false, append = false } = {}) {
    loading.value = true
    error.value = null
    try {
      const data = recommended ? await getPolicyRecommendations() : await getPolicies(params)
      list.value =
        append && list.value ? { ...data, content: [...list.value.content, ...data.content] } : data
      return data
    } catch (nextError) {
      error.value = nextError
      return null
    } finally {
      loading.value = false
    }
  }

  async function fetchDetail(policyId) {
    detailLoading.value = true
    detailError.value = null
    detail.value = null
    try {
      detail.value = await getPolicy(policyId)
      return detail.value
    } catch (nextError) {
      detailError.value = nextError
      return null
    } finally {
      detailLoading.value = false
    }
  }

  return {
    list,
    detail,
    loading,
    detailLoading,
    error,
    detailError,
    fetchList,
    fetchDetail,
  }
})
