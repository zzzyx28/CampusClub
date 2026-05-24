/**
 * JWT等工具，用于解析JWT token，获取映射的社团活跃度名称
 */

export function parseJwt(token) {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );

    return JSON.parse(jsonPayload);
  } catch (error) {
    console.error('JWT解析失败:', error);
    return {};
  }
}

export function getRoleName(userType) {
  switch (userType) {
    case 1:
      return 'user';
    case 2:
      return 'service';
    case 3:
      return 'admin';
    default:
      return 'user';
  }
}

export function getMemberLevelName(memberLevel) {
  switch (memberLevel) {
    case 1:
      return '新成员';
    case 2:
      return '活跃';
    case 3:
      return '骨干';
    case 4:
      return '核心';
    case 5:
      return '荣誉';
    default:
      return '新成员';
  }
}
