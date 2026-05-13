import { ref, computed } from 'vue'

const COIN_TO_RMB = 400000
const isRmb = ref(localStorage.getItem('currencyMode') === 'rmb')

export function useCurrency() {
  const currencyLabel = computed(() => isRmb.value ? '¥' : '')
  const currencyUnit = computed(() => isRmb.value ? '元' : '游戏币')

  function toggleCurrency() {
    isRmb.value = !isRmb.value
    localStorage.setItem('currencyMode', isRmb.value ? 'rmb' : 'coin')
  }

  function formatMoney(val) {
    if (val == null) return '0.00'
    if (isRmb.value) {
      const rmb = Number(val) / COIN_TO_RMB
      return rmb.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    }
    const num = Number(val)
    const abs = Math.abs(num)
    const sign = num < 0 ? '-' : ''
    if (abs >= 1e6) return sign + (abs / 1e6).toFixed(1) + 'm'
    if (abs >= 1e3) return sign + (abs / 1e3).toFixed(1) + 'k'
    return sign + abs.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  }

  function toRmb(val) {
    if (val == null) return 0
    return Number(val) / COIN_TO_RMB
  }

  return {
    isRmb,
    currencyLabel,
    currencyUnit,
    toggleCurrency,
    formatMoney,
    toRmb
  }
}
