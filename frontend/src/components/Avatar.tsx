import { cn } from '@/lib/utils'

type AvatarSize = 24 | 32 | 40 | 56

interface AvatarProps {
  src?: string
  initials?: string
  label?: string
  size?: AvatarSize
  className?: string
}

const SIZE_CLASSES: Record<AvatarSize, string> = {
  24: 'h-6 w-6',
  32: 'h-8 w-8',
  40: 'h-10 w-10',
  56: 'h-14 w-14',
}

const TEXT_CLASSES: Record<AvatarSize, string> = {
  24: 'text-[0.625rem]',
  32: 'text-[0.75rem]',
  40: 'text-[0.875rem]',
  56: 'text-1rem',
}

export default function Avatar({
  src,
  initials,
  label = 'Avatar',
  size = 40,
  className,
}: AvatarProps) {
  const sizeClass = SIZE_CLASSES[size]
  const textClass = TEXT_CLASSES[size]

  const computedInitials = initials || (label ? label.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2) : '?')

  return (
    <span
      aria-label={label}
      className={cn(
        'relative inline-flex shrink-0 items-center justify-center overflow-hidden border border-[var(--line)] rounded-full bg-[color-mix(in_oklab,var(--surface-strong)88%,white_12%)]',
        sizeClass,
        className,
      )}
    >
      {src ? (
        <img
          src={src}
          alt=""
          className="h-full w-full object-cover"
          width={size === 56 ? 112 : size === 40 ? 80 : size === 32 ? 64 : 48}
          height={size === 56 ? 112 : size === 40 ? 80 : size === 32 ? 64 : 48}
          decoding="async"
          loading="lazy"
        />
      ) : (
        <span className={cn('text-[var(--sea-ink-soft)] font-semibold', textClass)}>
          {computedInitials}
        </span>
      )}
    </span>
  )
}
