import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  getBillOcrResult,
  getDiagnosis,
  getDiagnosisMonths,
  startBillOcr,
} from '@/api/analysis'
import { useAnalysisStore } from '@/stores/analysis'

vi.mock('@/api/analysis', () => ({
  checkBillDuplicates: vi.fn(),
  createBill: vi.fn(),
  getBillOcrResult: vi.fn(),
  getBillTargetMonth: vi.fn(),
  getDiagnosis: vi.fn(),
  getDiagnosisMonths: vi.fn(),
  startBillOcr: vi.fn(),
}))

describe('analysis store — OCR', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.useFakeTimers()
    vi.clearAllMocks()
  })

  it('이미지를 업로드하고 완료될 때까지 폴링한다', async () => {
    const file = new File(['bill'], 'bill.jpg', { type: 'image/jpeg' })
    const completed = {
      jobId: 'ocr_test',
      status: 'SUCCEEDED',
      progress: 100,
      billType: 'ELECTRICITY',
      billingMonth: '2026-08',
      items: [],
    }
    startBillOcr.mockResolvedValue({
      jobId: 'ocr_test',
      status: 'PENDING',
      progress: 0,
      pollAfterMs: 1000,
    })
    getBillOcrResult
      .mockResolvedValueOnce({ jobId: 'ocr_test', status: 'RUNNING', progress: 50 })
      .mockResolvedValueOnce(completed)

    const store = useAnalysisStore()
    store.selectImage(file)
    const analyzing = store.analyzeSelectedImage('2026-08')

    await vi.advanceTimersByTimeAsync(2000)

    await expect(analyzing).resolves.toEqual(completed)
    expect(startBillOcr).toHaveBeenCalledWith({ image: file, billingMonthHint: '2026-08' })
    expect(getBillOcrResult).toHaveBeenCalledTimes(2)
    expect(store.ocrProgress).toBe(100)
    expect(store.ocrResult).toEqual(completed)
    expect(store.ocrError).toBeNull()
  })

  it('OCR 실패 응답의 안내 문구를 보관한다', async () => {
    const file = new File(['bill'], 'bill.png', { type: 'image/png' })
    startBillOcr.mockResolvedValue({
      jobId: 'ocr_failed',
      status: 'PENDING',
      progress: 0,
      pollAfterMs: 1000,
    })
    getBillOcrResult.mockResolvedValue({
      jobId: 'ocr_failed',
      status: 'FAILED',
      errorCode: 'OCR_FAILED',
      message: '사진에서 값을 읽지 못했어요.',
    })

    const store = useAnalysisStore()
    store.selectImage(file)
    const analyzing = store.analyzeSelectedImage('2026-08')

    await vi.advanceTimersByTimeAsync(1000)

    await expect(analyzing).resolves.toBeNull()
    expect(store.ocrError).toMatchObject({
      code: 'OCR_FAILED',
      message: '사진에서 값을 읽지 못했어요.',
    })
  })
})

describe('analysis store — diagnosis month', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('선택한 월의 진단과 등록된 월 목록을 조회한다', async () => {
    getDiagnosis.mockResolvedValue({ empty: false, yearMonth: '2026-07', summary: {} })
    getDiagnosisMonths.mockResolvedValue({
      months: [{ yearMonth: '2026-07', registered: true }],
      defaultMonth: '2026-07',
    })

    const store = useAnalysisStore()

    await store.fetchHome('2026-07')
    await store.fetchDiagnosisMonths()

    expect(getDiagnosis).toHaveBeenCalledWith({ month: '2026-07' })
    expect(getDiagnosisMonths).toHaveBeenCalledOnce()
    expect(store.diagnosisMonths).toEqual([{ yearMonth: '2026-07', registered: true }])
  })
})
