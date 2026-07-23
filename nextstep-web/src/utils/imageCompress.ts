/**
 * 图片预压缩工具：把超大图压到 1500px 长边、JPG 75%，避免 base64 后请求体过大、API 慢
 *
 * - PDF 直接放过（后端会用 PDFBox 转图，前端无法处理）
 * - 已经很小的图（<300KB）也放过，避免无意义重压
 * - 异常时回退原图，不阻塞用户上传
 */
const MAX_DIM = 1500
const QUALITY = 0.75
const SKIP_THRESHOLD = 300 * 1024  // < 300KB 不压缩

export async function compressImageIfNeeded(file: File): Promise<File> {
  if (!file.type.startsWith('image/')) return file
  if (file.size < SKIP_THRESHOLD) return file

  try {
    const bitmap = await createImageBitmap(file)
    const { width, height } = bitmap
    const scale = Math.min(1, MAX_DIM / Math.max(width, height))
    if (scale === 1 && file.size < 1.5 * 1024 * 1024) {
      bitmap.close()
      return file
    }

    const w = Math.round(width * scale)
    const h = Math.round(height * scale)
    const canvas = document.createElement('canvas')
    canvas.width = w
    canvas.height = h
    const ctx = canvas.getContext('2d')!
    ctx.drawImage(bitmap, 0, 0, w, h)
    bitmap.close()

    const blob: Blob | null = await new Promise(resolve =>
      canvas.toBlob(resolve, 'image/jpeg', QUALITY)
    )
    if (!blob) return file
    if (blob.size >= file.size) return file  // 压缩后反而更大就用原图

    const newName = file.name.replace(/\.(png|jpe?g|webp|bmp|gif)$/i, '.jpg') || 'compressed.jpg'
    return new File([blob], newName, { type: 'image/jpeg', lastModified: Date.now() })
  } catch {
    return file
  }
}
