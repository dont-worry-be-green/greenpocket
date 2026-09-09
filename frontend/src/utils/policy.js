export const CURRENT_STATUS_OPTIONS = [
  { value: 'EMPLOYED', label: '재직 중' },
  { value: 'SELF_EMPLOYED', label: '자영업' },
  { value: 'UNEMPLOYED', label: '미취업' },
  { value: 'FREELANCER', label: '프리랜서' },
  { value: 'STUDENT', label: '학생' },
  { value: 'PREPARING_STARTUP', label: '창업 준비 중' },
  { value: 'OTHER', label: '기타' },
]

export const ANNUAL_INCOME_OPTIONS = [
  { value: 'NO_INCOME', label: '소득 없음' },
  { value: 'UNDER_24M', label: '2,400만원 미만' },
  { value: 'FROM_24M_TO_36M', label: '2,400만~3,600만원' },
  { value: 'FROM_36M_TO_50M', label: '3,600만~5,000만원' },
  { value: 'OVER_50M', label: '5,000만원 이상' },
  { value: 'UNKNOWN', label: '잘 모르겠어요' },
]

export const HOUSEHOLD_STATUS_OPTIONS = [
  { value: 'ONE_PERSON', label: '1인 가구' },
  { value: 'WITH_PARENTS', label: '부모님과 거주' },
  { value: 'MARRIED', label: '기혼 가구' },
  { value: 'SINGLE_PARENT', label: '한부모 가구' },
  { value: 'OTHER', label: '기타' },
]

export const EDUCATION_STATUS_OPTIONS = [
  { value: 'BELOW_HIGH_SCHOOL', label: '고졸 미만' },
  { value: 'HIGH_SCHOOL_STUDENT', label: '고교 재학' },
  { value: 'HIGH_SCHOOL_EXPECTED_GRADUATION', label: '고졸 예정' },
  { value: 'HIGH_SCHOOL_GRADUATE', label: '고교 졸업' },
  { value: 'UNIVERSITY_STUDENT', label: '대학 재학' },
  { value: 'UNIVERSITY_EXPECTED_GRADUATION', label: '대졸 예정' },
  { value: 'UNIVERSITY_GRADUATE', label: '대학 졸업' },
  { value: 'GRADUATE_SCHOOL', label: '석·박사' },
  { value: 'OTHER', label: '기타' },
]

export const POLICY_CATEGORY_OPTIONS = [
  { value: '', label: '모든 정책 분야' },
  { value: 'JOB', label: '일자리' },
  { value: 'HOUSING', label: '주거' },
  { value: 'EDUCATION', label: '교육' },
  { value: 'WELFARE_CULTURE', label: '복지·문화' },
  { value: 'PARTICIPATION_RIGHTS', label: '참여·권리' },
]

export const APPLICATION_STATUS_OPTIONS = [
  { value: '', label: '전체 상태' },
  { value: 'OPEN', label: '신청 가능' },
  { value: 'UPCOMING', label: '신청 예정' },
  { value: 'CLOSED', label: '신청 마감' },
  { value: 'UNKNOWN', label: '일정 확인' },
]

const labelOf = (options, value) => options.find((item) => item.value === value)?.label ?? '-'

export const formatCurrentStatus = (value) => labelOf(CURRENT_STATUS_OPTIONS, value)
export const formatAnnualIncomeBand = (value) => labelOf(ANNUAL_INCOME_OPTIONS, value)
export const formatHouseholdStatus = (value) => labelOf(HOUSEHOLD_STATUS_OPTIONS, value)
export const formatEducationStatus = (value) => labelOf(EDUCATION_STATUS_OPTIONS, value)
export const formatPolicyCategory = (value) => labelOf(POLICY_CATEGORY_OPTIONS, value)
export const formatApplicationStatus = (value) => labelOf(APPLICATION_STATUS_OPTIONS, value)

export function policyMatchMeta(status) {
  return (
    {
      ELIGIBLE: { label: '신청 가능성이 높아요', tone: 'positive' },
      CHECK_REQUIRED: { label: '세부 조건 확인 필요', tone: 'confirmed' },
      NOT_ELIGIBLE: { label: '조건이 맞지 않아요', tone: 'sub' },
    }[status] ?? { label: '조건을 확인해 주세요', tone: 'sub' }
  )
}

export function formatPolicyDate(value) {
  if (!value) return '일정 확인'
  const [year, month, day] = value.split('-')
  return `${year}.${month}.${day}`
}

export function formatPolicyDeadline(applicationStatus, endDate) {
  if (applicationStatus === 'OPEN' && !endDate) return '상시 신청'
  if (!endDate) return '신청 기간 확인'
  return `${formatPolicyDate(endDate)}까지`
}

export function formatPolicyPeriod(application) {
  if (application?.periodType === 'ALWAYS') return '상시 신청'
  if (application?.startDate && application?.endDate) {
    return `${formatPolicyDate(application.startDate)} ~ ${formatPolicyDate(application.endDate)}`
  }
  if (application?.startDate) return `${formatPolicyDate(application.startDate)}부터`
  if (application?.endDate) return `${formatPolicyDate(application.endDate)}까지`
  return '신청 기간 확인'
}
