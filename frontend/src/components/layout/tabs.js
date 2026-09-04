import IconChart from '@/components/ui/icons/IconChart.vue'
import IconGift from '@/components/ui/icons/IconGift.vue'
import IconLeaf from '@/components/ui/icons/IconLeaf.vue'
import IconPocket from '@/components/ui/icons/IconPocket.vue'
import IconUser from '@/components/ui/icons/IconUser.vue'

/*
 * 하단 탭 5개 (기능명세서 COM-02)
 *
 * 라벨과 순서는 결정 B-2 로 확정된 것이다. 시안마다 분석·홈·Green-what-if 로 달리 적혀 있어도
 * 아래 표기로 통일한다. **여기 말고 다른 곳에서 탭 목록을 만들지 않는다.**
 *
 * 경로는 폴더명(eco·greenlife)이 아니라 **화면 ID 기준**이다.
 *   /whatif → views/eco (WF) · /benefit → views/greenlife (BN)
 *
 * key 는 AppTabLayout 의 tab prop 과 GpTabBar 의 active 가 함께 쓴다.
 */
export const TABS = [
  { key: 'analysis', label: '진단', path: '/analysis', icon: IconChart },
  { key: 'benefit', label: '혜택', path: '/benefit', icon: IconGift },
  // fab: 가운데 솟은 원형 버튼. 온보딩 완료 후 항상 여기로 진입한다 (결정 C-1)
  { key: 'whatif', label: 'What-if', path: '/whatif', icon: IconLeaf, fab: true },
  { key: 'pocket', label: '포켓', path: '/pocket', icon: IconPocket },
  { key: 'mypage', label: '마이페이지', path: '/mypage', icon: IconUser },
]

export function findTab(key) {
  return TABS.find((t) => t.key === key)
}
