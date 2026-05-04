export const ROLE_TEXT_MAP = {
  0: '普通用户',
  1: '会员',
  2: '管理员',
  3: '香草管理员'
}

export function roleText(role) {
  return ROLE_TEXT_MAP[Number(role)] || '未知角色'
}

export function isAdminRole(role) {
  return Number(role) >= 2
}

export function isSuperAdminRole(role) {
  return Number(role) >= 3
}
