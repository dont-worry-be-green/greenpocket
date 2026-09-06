<script setup>
defineProps({
  bankCode: { type: String, default: null },
  bankName: { type: String, default: null },
  size: { type: Number, default: 36 },
})

const BANK_META = {
  '088': { label: '신한', bg: '#0046FF', color: '#fff' },
  '004': { label: 'KB', bg: '#FFB900', color: '#1A1A1A' },
  '020': { label: '우리', bg: '#007BC2', color: '#fff' },
  '081': { label: '하나', bg: '#009B71', color: '#fff' },
  '011': { label: '농협', bg: '#00873D', color: '#fff' },
  '003': { label: '기업', bg: '#004EA2', color: '#fff' },
  '090': { label: '카카오', bg: '#FAE100', color: '#1A1A1A' },
  '092': { label: '토스', bg: '#0064FF', color: '#fff' },
}

const NAME_TO_CODE = {
  '신한은행': '088', 'KB국민은행': '004', '우리은행': '020',
  '하나은행': '081', 'NH농협은행': '011', 'IBK기업은행': '003',
  '카카오뱅크': '090', '토스뱅크': '092',
}

function meta(bankCode, bankName) {
  const code = bankCode ?? NAME_TO_CODE[bankName]
  return BANK_META[code] ?? { label: '은행', bg: '#c4c4c4', color: '#fff' }
}
</script>

<template>
  <span
    :style="{
      width: size + 'px',
      height: size + 'px',
      background: meta(bankCode, bankName).bg,
      color: meta(bankCode, bankName).color,
      fontSize: size * 0.32 + 'px',
      borderRadius: '8px',
      display: 'inline-flex',
      alignItems: 'center',
      justifyContent: 'center',
      fontWeight: 700,
      lineHeight: 1,
      flexShrink: 0,
      letterSpacing: '-0.02em',
    }"
    aria-hidden="true"
  >{{ meta(bankCode, bankName).label }}</span>
</template>
