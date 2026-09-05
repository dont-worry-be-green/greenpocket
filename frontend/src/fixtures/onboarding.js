/*
 * 온보딩 화면 미리보기 데이터 — **API 연동 전까지만 쓴다.**
 *
 * 필드명·타입은 docs/api/api-spec.md 4·5절 응답 예시를 그대로 따른다.
 *   buildUserStart      ← POST /users        4.1 (COM-01)
 *   SIDOS · SEOUL_SIGUNGUS ← GET /meta/regions 4.3 (A-1-01)
 *   buildProfileResult  ← POST /profile      5.1 (A-1-05)
 *
 * 백엔드 대조(origin/main 8654695 기준):
 *   POST /users        구현되어 있다 — `api/onboarding.js` 가 픽스처 모드에서도 실제로 부른다
 *   GET  /meta/regions 컨트롤러 없음
 *   POST /profile      profile 패키지 없음
 *
 * **값이나 필드를 임의로 만들지 않는다.** 새 필드가 필요하면 api-spec.md 를 먼저 고친다
 * (frontend/AGENTS.md 9절). 여기의 `SIDO_SHORT_NAMES` 와 라벨 맵은 응답 필드가 아니라
 * `profileSummary` 문자열을 서버처럼 조립하기 위한 재료다.
 */

/** GET /meta/regions (sidoCode 없음) — level: 'SIDO'. 행정표준코드 17개 */
export const SIDOS = [
  { code: '11', name: '서울특별시' },
  { code: '26', name: '부산광역시' },
  { code: '27', name: '대구광역시' },
  { code: '28', name: '인천광역시' },
  { code: '29', name: '광주광역시' },
  { code: '30', name: '대전광역시' },
  { code: '31', name: '울산광역시' },
  { code: '36', name: '세종특별자치시' },
  { code: '41', name: '경기도' },
  { code: '43', name: '충청북도' },
  { code: '44', name: '충청남도' },
  { code: '46', name: '전라남도' },
  { code: '47', name: '경상북도' },
  { code: '48', name: '경상남도' },
  { code: '50', name: '제주특별자치도' },
  { code: '51', name: '강원특별자치도' },
  { code: '52', name: '전북특별자치도' },
]

/*
 * GET /meta/regions?sidoCode=11 — level: 'SIGUNGU'. 서울 25개 자치구.
 *
 * ⚠️ `hasRegionAverage` 가 **관악구(11620)만 true 다.** 지역 평균의 근거가 되는
 * `region_utility_snapshot` 시드가 아직 없고, 문서에 값이 적힌 지역이 관악구뿐이다
 * (api-spec.md 4.3 예시). 나머지에 true 를 넣으면 화면이 없는 근거로 숫자를 약속하게 된다
 * (핵심 비즈니스 규칙 8 — 불확실한 수치 미표시). 시드가 들어오면 서버 값으로 바뀐다.
 */
export const SEOUL_SIGUNGUS = [
  { code: '11110', name: '종로구', sidoCode: '11', hasRegionAverage: false },
  { code: '11140', name: '중구', sidoCode: '11', hasRegionAverage: false },
  { code: '11170', name: '용산구', sidoCode: '11', hasRegionAverage: false },
  { code: '11200', name: '성동구', sidoCode: '11', hasRegionAverage: false },
  { code: '11215', name: '광진구', sidoCode: '11', hasRegionAverage: false },
  { code: '11230', name: '동대문구', sidoCode: '11', hasRegionAverage: false },
  { code: '11260', name: '중랑구', sidoCode: '11', hasRegionAverage: false },
  { code: '11290', name: '성북구', sidoCode: '11', hasRegionAverage: false },
  { code: '11305', name: '강북구', sidoCode: '11', hasRegionAverage: false },
  { code: '11320', name: '도봉구', sidoCode: '11', hasRegionAverage: false },
  { code: '11350', name: '노원구', sidoCode: '11', hasRegionAverage: false },
  { code: '11380', name: '은평구', sidoCode: '11', hasRegionAverage: false },
  { code: '11410', name: '서대문구', sidoCode: '11', hasRegionAverage: false },
  { code: '11440', name: '마포구', sidoCode: '11', hasRegionAverage: false },
  { code: '11470', name: '양천구', sidoCode: '11', hasRegionAverage: false },
  { code: '11500', name: '강서구', sidoCode: '11', hasRegionAverage: false },
  { code: '11530', name: '구로구', sidoCode: '11', hasRegionAverage: false },
  { code: '11545', name: '금천구', sidoCode: '11', hasRegionAverage: false },
  { code: '11560', name: '영등포구', sidoCode: '11', hasRegionAverage: false },
  { code: '11590', name: '동작구', sidoCode: '11', hasRegionAverage: false },
  { code: '11620', name: '관악구', sidoCode: '11', hasRegionAverage: true },
  { code: '11650', name: '서초구', sidoCode: '11', hasRegionAverage: false },
  { code: '11680', name: '강남구', sidoCode: '11', hasRegionAverage: false },
  { code: '11710', name: '송파구', sidoCode: '11', hasRegionAverage: false },
  { code: '11740', name: '강동구', sidoCode: '11', hasRegionAverage: false },
]

/** `profileSummary` 앞머리. "서울특별시 관악구"가 아니라 "서울 관악구"다(api-spec.md 5.1 예시) */
const SIDO_SHORT_NAMES = {
  11: '서울',
  26: '부산',
  27: '대구',
  28: '인천',
  29: '광주',
  30: '대전',
  31: '울산',
  36: '세종',
  41: '경기',
  43: '충북',
  44: '충남',
  46: '전남',
  47: '경북',
  48: '경남',
  50: '제주',
  51: '강원',
  52: '전북',
}

/** HousingType — api-spec.md 3절 · schema.sql `app_user.housing_type` */
const HOUSING_TYPE_LABELS = {
  ONE_ROOM: '원룸',
  OFFICETEL: '오피스텔',
  APARTMENT: '아파트',
  MULTI_HOUSE: '다세대',
}

/** AreaBand — api-spec.md 3절 · schema.sql `app_user.area_band` */
const AREA_BAND_LABELS = {
  UNDER_10: '10평 이하',
  FROM_10_TO_20: '10~20평',
  OVER_20: '20평 이상',
}

/*
 * 그린포켓 계좌번호 `1005-####-####-##` (결정 C-14).
 * demoKey 에서 뽑아 **같은 키면 같은 번호**가 나오게 한다 — 재진입 시 번호가 바뀌면
 * 서버가 사용자별 고유 번호를 발급한다는 약속(api-spec.md 4.1)과 어긋난다.
 */
function pocketAccountNo(demoKey) {
  let hash = 0
  for (const char of String(demoKey)) hash = (hash * 31 + char.codePointAt(0)) % 1_000_000_000
  const digits = String(hash).padStart(10, '0').slice(-10)
  return `1005-${digits.slice(0, 4)}-${digits.slice(4, 8)}-${digits.slice(8, 10)}`
}

/** POST /users 201 — ONB-01 (COM-01). 서버가 꺼져 있을 때만 쓰인다 */
export function buildUserStart({ demoKey, name }) {
  return {
    userId: 1,
    name,
    onboardingCompleted: false,
    nextScreen: 'ONB-02',
    pocketAccountNo: pocketAccountNo(demoKey),
    pocketHolder: name,
    createdAt: new Date().toISOString(),
  }
}

/*
 * POST /profile 200 — ONB-02 (A-1-05).
 *
 * **상수가 아니라 조립 함수다.** 고른 지역·주거형태·평수가 요약 문자열에 그대로 나와야
 * A-1-07("동일 값이 화면마다 다르게 표시되지 않는다")을 흉내 낼 수 있다.
 */
export function buildProfileResult({ sidoCode, sigunguName, housingType, areaBand }) {
  const sido = SIDO_SHORT_NAMES[sidoCode] ?? ''
  const housing = HOUSING_TYPE_LABELS[housingType] ?? ''
  const area = AREA_BAND_LABELS[areaBand] ?? ''
  return {
    onboardingCompleted: true,
    profileSummary: `${sido} ${sigunguName} · ${housing} ${area}`,
    nextScreen: 'WF-06',
    // seoulResident = sidoCode == "11" (api-spec.md 5.1). B-1-09 서울 거주 판정의 근거다
    seoulResident: sidoCode === '11',
  }
}
