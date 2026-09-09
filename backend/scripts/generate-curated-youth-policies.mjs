import fs from 'node:fs'
import path from 'node:path'

const curatedIds = [
  '20260311005400112111', '20260310005400112102', '20250704005400111154',
  '20260422005400212862', '20260724005400213300', '20260422005400212867',
  '20260504005400213065', '20260504005400213021', '20260504005400213018',
  '20260429005400212911', '20260429005400212910', '20260421005400212789',
  '20251124005400211936', '20251219005400212033', '20260421005400212830',
  '20260421005400212784', '20260413005400212724', '20260406005400212458',
  '20260331005400212365', '20260107005400212072', '20260429005400212904',
  '20260421005400212786', '20260325005400212269', '20260313005400212124',
  '20260504005400113081', '20260430005400212957', '20260430005400212958',
  '20260413005400212694', '20260409005400212657', '20260409005400212656',
  '20260406005400212460', '20260504005400213057', '20260320005400112238',
  '20250617005400110950', '20260903005400113371', '20260320005400112239',
  '20260320005400112237', '20260316005400112166', '20251124005400211935',
  '20260319005400212235', '20260304005400212098', '20260504005400113095',
  '20260504005400113092', '20260504005400113135', '20260724005400113307',
  '20260421005400112773', '20260415005400112751', '20260724005400213306',
  '20260310005400112106', '20260504005400213013', '20260430005400212954',
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
  }
})

fs.mkdirSync(path.dirname(outputFile), { recursive: true })
fs.writeFileSync(outputFile, `${JSON.stringify(snapshot, null, 2)}\n`)
console.log(`Wrote ${snapshot.length} curated policies to ${outputFile}`)
