import { getToken } from './http'

/** 导出综合决策报告 PDF，并触发浏览器下载。 */
export const reportApi = {
  exportPdf: async () => {
    const token = getToken()
    const baseUrl = import.meta.env.DEV ? 'http://localhost:8080' : ''
    const resp = await fetch(`${baseUrl}/api/report/export`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    })
    const contentType = resp.headers.get('content-type') || ''
    if (!resp.ok || !contentType.includes('application/pdf')) {
      let message = `导出失败：HTTP ${resp.status}`
      try {
        const body = await resp.json() as { msg?: string }
        if (body.msg) message = body.msg
      } catch {}
      throw new Error(message)
    }

    const blob = await resp.blob()
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    const disposition = resp.headers.get('content-disposition') || ''
    const encodedName = disposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1]
    anchor.download = encodedName
      ? decodeURIComponent(encodedName)
      : `NextStep综合决策报告-${new Date().toLocaleDateString('sv-SE')}.pdf`
    document.body.appendChild(anchor)
    anchor.click()
    document.body.removeChild(anchor)
    URL.revokeObjectURL(url)
  }
}
