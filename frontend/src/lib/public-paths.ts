/** Routes that render without the app header/footer (public marketing + auth). */
export const PUBLIC_PATHS = ['/welcome', '/login'] as const

/** Whether a pathname should hide the authenticated app chrome. */
export function isPublicPath(pathname: string): boolean {
  return (PUBLIC_PATHS as readonly string[]).includes(pathname)
}
