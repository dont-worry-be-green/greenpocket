import fs from 'node:fs'
import path from 'node:path'

const curatedIds = [
  '20260311005400112111', '20260310005400112102', '20250704005400111154',
  '20260422005400212862', '20260724005400213300', '20260422005400212867',
  '20260504005400213065', '20260504005400213021', '20260504005400213018',
  '20260429005400212911', '20260429005400212910', '20260421005400212789',
  '20251124005400211936', '20251219005400212033', '20260421005400212830',
  '20260421005400212784', '20260413005400212724', '20260406005400212458',
  '20260331005400212365', '20260107005400212072', '20250226005400110565',
  '20250316005400210626', '20250316005400210631', '20250316005400210632',
  '20250316005400210633', '20260430005400212957', '20260430005400212958',
  '20260326005400212297', '20260409005400212657', '20260409005400212656',
  '20260406005400212460', '20260504005400213057', '20260320005400112238',
  '20250617005400110950', '20260903005400113371', '20260506005400213157',
  '20260320005400112237', '20260316005400112166', '20251124005400211935',
  '20260319005400212235', '20260304005400212098', '20260504005400113095',
  '20260504005400113092', '20260504005400113135', '20260724005400113307',
  '20260421005400112773', '20260415005400112751', '20260724005400213306',
  '20260506005400213144', '20260504005400213013', '20260430005400212981',
  '20260428005400212899', '20260422005400212834', '20260428005400212894',
  '20260424005400212874', '20260422005400212837', '20260318005400212198',
  '20260421005400212826', '20260421005400212824', '20260324005400212252',
]

if (curatedIds.length !== 60 || new Set(curatedIds).size !== 60) {
  throw new Error('The curated policy catalog must contain exactly 60 unique IDs.')
}

const inputDirectory = process.argv[2]
const outputFile = process.argv[3]
if (!inputDirectory || !outputFile) {
  throw new Error('Usage: node generate-curated-youth-policies.mjs <response-dir> <output-file>')
}

const sourceById = new Map()
for (const name of fs.readdirSync(inputDirectory)) {
  if (!/^youth-policy-page-(?:[0-9]+)\.json$/.test(name)) continue
  try {
    const response = JSON.parse(fs.readFileSync(path.join(inputDirectory, name), 'utf8'))
    for (const policy of response.result?.youthPolicyList ?? []) {
      if (curatedIds.includes(policy.plcyNo)) sourceById.set(policy.plcyNo, policy)
    }
  } catch {
    // Failed API response pages are intentionally ignored; missing IDs fail below.
  }
}

const text = (policy, key) => {
  const value = String(policy[key] ?? '').trim()
  return value || null
}
const date = (policy, key) => {
  const value = text(policy, key)
  return value && /^\d{8}$/.test(value)
    ? `${value.slice(0, 4)}-${value.slice(4, 6)}-${value.slice(6, 8)}`
    : null
}
const dateTime = (policy, key) => text(policy, key)?.replace(' ', 'T') ?? null
const positiveNumber = (policy, key, maximum = Number.MAX_SAFE_INTEGER) => {
  const value = Number(text(policy, key))
  return Number.isInteger(value) && value > 0 && value <= maximum ? value : null
}

/*
 * 온통청년 원본은 계속 운영 중인 제도를 과거 사업연도 종료로 내려 주는 경우가 있다.
 * 아래 다섯 건은 서울·전국 청년이 실제로 탐색할 주거 제도이므로, 검증한 공식 안내
 * 경로와 상시 제도 상태를 스냅샷에 고정한다. 원본의 대상·지원 내용은 그대로 보존한다.
 */
const verifiedHousingOverrides = new Map([
  ['20250226005400110565', {
    applicationPeriodCode: '0057002',
    applicationDateText: null,
    applicationMethod: '주택도시기금 수탁은행에서 가입하거나 주택도시기금 안내 페이지에서 취급은행을 확인해 주세요.',
    applicationUrl: 'https://nhuf.molit.go.kr/FP/FP07/FP0701/FP07010301.jsp',
    additionalConditionText: '만 19~34세, 연소득 5,000만원 이하인 무주택 청년',
  }],
  ['20250316005400210626', {
    applicationPeriodCode: '0057002',
    applicationDateText: null,
    applicationMethod: '서울주택도시개발공사 공고에서 모집 주택과 접수 일정을 확인한 뒤 신청해 주세요.',
    applicationUrl: 'https://www.i-sh.co.kr/',
    marriageStatusCode: '0055002',
    additionalConditionText: '무주택 저소득 청년(대학생·취업준비생 등)으로 소득·자산 기준 충족 필요',
  }],
  ['20250316005400210631', {
    applicationPeriodCode: '0057002',
    applicationDateText: null,
    applicationMethod: '청년몽땅정보통의 청년 부동산 중개보수 및 이사비 지원 페이지에서 모집 일정을 확인해 신청해 주세요.',
    applicationUrl: 'https://youth.seoul.go.kr/youthConts.do?key=2310100044',
    incomeConditionText: '가구당 기준 중위소득 150% 이하',
    additionalConditionText: '2024.1.1. 이후 서울 전입 또는 서울 내 이사 후 전입신고를 마친 무주택 청년가구이며 거래금액 2억원 이하 전·월세 거주자',
  }],
  ['20250316005400210632', {
    applicationPeriodCode: '0057002',
    applicationDateText: null,
    applicationMethod: '청년안심주택 모집공고에서 공급 주택과 접수 일정을 확인한 뒤 신청해 주세요.',
    applicationUrl: 'https://soco.seoul.go.kr/youth/pgm/home/yohome/supportYouth1.do?menuNo=400039',
    marriageStatusCode: '0055002',
    incomeConditionText: '공급유형별 전년도 도시근로자 월평균소득 기준 충족 필요',
    additionalConditionText: '만 19~39세 청년으로 무주택·자동차가액·자산 기준 충족 필요(청년형은 미혼)',
  }],
  ['20250316005400210633', {
    applicationPeriodCode: '0057002',
    applicationDateText: null,
    applicationMethod: '청년안심주택 임대차계약 체결 후 소득심사에 필요한 서류를 제출해 주세요.',
    applicationUrl: 'https://soco.seoul.go.kr/',
    incomeConditionText: '전년도 도시근로자 가구당 월평균소득 100% 이하',
    additionalConditionText: '청년안심주택 민간임대 입주(예정)자로서 청년 자산 2억 7,300만원 이하 등 세부 기준 충족 필요',
  }],
])

const missingIds = curatedIds.filter((id) => !sourceById.has(id))
if (missingIds.length) throw new Error(`Missing curated policies: ${missingIds.join(', ')}`)

const snapshot = curatedIds.map((id) => {
  const policy = sourceById.get(id)
  return {
    externalPolicyId: text(policy, 'plcyNo'),
    title: text(policy, 'plcyNm'),
    keywordName: text(policy, 'plcyKywdNm'),
    description: text(policy, 'plcyExplnCn'),
    largeCategoryName: text(policy, 'lclsfNm'),
    mediumCategoryName: text(policy, 'mclsfNm'),
    supportContent: text(policy, 'plcySprtCn'),
    supervisingOrgName: text(policy, 'sprvsnInstCdNm'),
    operatingOrgName: text(policy, 'operInstCdNm'),
    approvalStatusCode: text(policy, 'plcyAprvSttsCd'),
    provisionMethodCode: text(policy, 'plcyPvsnMthdCd'),
    applicationPeriodCode: text(policy, 'aplyPrdSeCd'),
    businessStartDate: date(policy, 'bizPrdBgngYmd'),
    businessEndDate: date(policy, 'bizPrdEndYmd'),
    applicationDateText: text(policy, 'aplyYmd'),
    applicationMethod: text(policy, 'plcyAplyMthdCn'),
    applicationUrl: text(policy, 'aplyUrlAddr'),
    referenceUrl1: text(policy, 'refUrlAddr1'),
    referenceUrl2: text(policy, 'refUrlAddr2'),
    ageLimitYn: text(policy, 'sprtTrgtAgeLmtYn'),
    minAge: positiveNumber(policy, 'sprtTrgtMinAge', 100),
    maxAge: positiveNumber(policy, 'sprtTrgtMaxAge', 100),
    marriageStatusCode: text(policy, 'mrgSttsCd'),
    incomeConditionCode: text(policy, 'earnCndSeCd'),
    incomeMinAmount: positiveNumber(policy, 'earnMinAmt'),
    incomeMaxAmount: positiveNumber(policy, 'earnMaxAmt'),
    incomeConditionText: text(policy, 'earnEtcCn'),
    additionalConditionText: text(policy, 'addAplyQlfcCndCn'),
    participantTargetText: text(policy, 'ptcpPrpTrgtCn'),
    regionCodes: text(policy, 'zipCd'),
    majorCodes: text(policy, 'plcyMajorCd'),
    employmentCodes: text(policy, 'jobCd'),
    schoolCodes: text(policy, 'schoolCd'),
    specialCodes: text(policy, 'sbizCd'),
    sourceRegisteredAt: dateTime(policy, 'frstRegDt'),
    sourceModifiedAt: dateTime(policy, 'lastMdfcnDt'),
    ...(verifiedHousingOverrides.get(id) ?? {}),
  }
})

const overlyBroadPolicies = snapshot.filter((policy) =>
  policy.minAge == null && policy.maxAge == null &&
  policy.incomeConditionCode === '0043001' &&
  policy.employmentCodes === '0013010' &&
  policy.schoolCodes === '0049010' &&
  policy.majorCodes === '0011009' &&
  policy.marriageStatusCode === '0055003' &&
  policy.specialCodes === '0014010' &&
  !policy.participantTargetText && !policy.additionalConditionText
)
if (overlyBroadPolicies.length) {
  throw new Error(`Curated policies need a specific target condition: ${overlyBroadPolicies.map((policy) => policy.externalPolicyId).join(', ')}`)
}

fs.mkdirSync(path.dirname(outputFile), { recursive: true })
fs.writeFileSync(outputFile, `${JSON.stringify(snapshot, null, 2)}\n`)
console.log(`Wrote ${snapshot.length} curated policies to ${outputFile}`)
