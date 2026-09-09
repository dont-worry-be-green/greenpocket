/*
 * 은행 표시 메타 — 로고 타일과 짧은 이름.
 *
 * `BankLogo` 와 포켓 홈 잔액 카드가 같은 표를 본다. 서버는 `bankCode`(금융결제원 코드)와
 * `bankName` 을 주고, 화면은 짧은 이름(「국민」)과 타일만 필요하다.
 *
 * 로고 이미지는 확정 시안(결정 C-31)에 들어간 KB국민은행 하나뿐이다. 나머지는 글자 타일로
 * 떨어진다 — 이미지가 없다고 빈 칸을 남기지 않는다.
 */
import kbLogo from '@/assets/bank/kb.png'

const BANK_META = {
  '088': { label: '신한', bg: '#0046FF', color: '#fff' },
  '004': { label: '국민', bg: '#FFBC00', color: '#1A1A1A', image: kbLogo },
  '020': { label: '우리', bg: '#007BC2', color: '#fff' },
  '081': { label: '하나', bg: '#009B71', color: '#fff' },
  '011': { label: '농협', bg: '#00873D', color: '#fff' },
  '003': { label: '기업', bg: '#004EA2', color: '#fff' },
  '090': { label: '카카오', bg: '#FAE100', color: '#1A1A1A' },
  '092': { label: '토스', bg: '#0064FF', color: '#fff' },
}

const NAME_TO_CODE = {
  신한은행: '088',
  KB국민은행: '004',
  우리은행: '020',
  하나은행: '081',
  NH농협은행: '011',
  IBK기업은행: '003',
  카카오뱅크: '090',
  토스뱅크: '092',
}

const FALLBACK = { label: '은행', bg: '#c4c4c4', color: '#fff' }

export function bankMeta(bankCode, bankName) {
  const code = bankCode ?? NAME_TO_CODE[bankName]
  return BANK_META[code] ?? FALLBACK
}

/** 「KB국민은행」 → 「국민」. 표에 없으면 서버 이름을 그대로 쓴다 */
export function bankShortName(bankCode, bankName) {
  const meta = bankMeta(bankCode, bankName)
  return meta === FALLBACK ? (bankName ?? FALLBACK.label) : meta.label
}
