// 폴더 트리(roots/children) 유틸

/** 트리를 깊이 우선 평면 목록으로 — 셀렉트 옵션·경로 표시용 [{ id, name, depth, path }] */
export function flattenFolders(roots, depth = 0, parentPath = '') {
  return roots.flatMap((f) => {
    const path = parentPath ? `${parentPath} / ${f.name}` : f.name
    return [{ id: f.id, name: f.name, depth, path }, ...flattenFolders(f.children ?? [], depth + 1, path)]
  })
}

/** 셀렉트 옵션 라벨 (들여쓰기) */
export const indentLabel = (f) => `${'　'.repeat(f.depth)}${f.depth ? '└ ' : ''}${f.name}`
