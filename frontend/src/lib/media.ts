/** Read a file as base64 so it can travel through a server function. */
export async function fileToBase64(file: File): Promise<string> {
  const bytes = new Uint8Array(await file.arrayBuffer())
  let binary = ''
  const chunkSize = 0x8000
  for (let index = 0; index < bytes.length; index += chunkSize) {
    binary += String.fromCharCode(...bytes.subarray(index, index + chunkSize))
  }
  return btoa(binary)
}

/** Map a picked file to the post type expected by the backend. */
export function postTypeFor(file: File): 'PHOTO' | 'VIDEO' {
  return file.type.startsWith('video/') ? 'VIDEO' : 'PHOTO'
}
