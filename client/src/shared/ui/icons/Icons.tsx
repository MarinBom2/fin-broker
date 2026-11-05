import type { SVGProps } from 'react'

/** 선 아이콘 모음. 굵기·모서리를 맞춘 직접 그린 아이콘이다. 색은 currentColor 를 따른다 */
type IconProps = SVGProps<SVGSVGElement> & { size?: number }

function base({ size = 20, ...props }: IconProps) {
  return {
    width: size,
    height: size,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 1.8,
    strokeLinecap: 'round' as const,
    strokeLinejoin: 'round' as const,
    'aria-hidden': true,
    ...props,
  }
}

export const HomeIcon = (p: IconProps) => (
  <svg {...base(p)}>
    <path d="M3 10.5 12 3l9 7.5" />
    <path d="M5 9.5V20a1 1 0 0 0 1 1h4v-6h4v6h4a1 1 0 0 0 1-1V9.5" />
  </svg>
)

export const BuildingIcon = (p: IconProps) => (
  <svg {...base(p)}>
    <path d="M4 21V5a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v16" />
    <path d="M16 9h2a2 2 0 0 1 2 2v10" />
    <path d="M3 21h18M8 7h4M8 11h4M8 15h4" />
  </svg>
)

export const ShieldIcon = (p: IconProps) => (
  <svg {...base(p)}>
    <path d="M12 3 4.5 6v5.5c0 4.6 3.1 8.4 7.5 9.5 4.4-1.1 7.5-4.9 7.5-9.5V6L12 3Z" />
    <path d="m8.8 12 2.2 2.2 4.3-4.4" />
  </svg>
)

export const PersonIcon = (p: IconProps) => (
  <svg {...base(p)}>
    <circle cx="12" cy="8" r="4" />
    <path d="M4 21a8 8 0 0 1 16 0" />
  </svg>
)

export const SearchIcon = (p: IconProps) => (
  <svg {...base(p)}>
    <circle cx="11" cy="11" r="7" />
    <path d="m20 20-3.5-3.5" />
  </svg>
)

export const ChevronRightIcon = (p: IconProps) => (
  <svg {...base({ size: 14, strokeWidth: 2.4, ...p })}>
    <path d="m9 5 7 7-7 7" />
  </svg>
)

export const ChevronLeftIcon = (p: IconProps) => (
  <svg {...base({ size: 14, strokeWidth: 2.4, ...p })}>
    <path d="m15 5-7 7 7 7" />
  </svg>
)

export const ExclamationIcon = (p: IconProps) => (
  <svg {...base(p)}>
    <path d="M10.3 3.9 2.4 17.6A2 2 0 0 0 4.1 20.6h15.8a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0Z" />
    <path d="M12 9v4.5M12 17h.01" />
  </svg>
)

export const BlockIcon = (p: IconProps) => (
  <svg {...base(p)}>
    <circle cx="12" cy="12" r="9" />
    <path d="m5.7 5.7 12.6 12.6" />
  </svg>
)

export const CheckCircleIcon = (p: IconProps) => (
  <svg {...base(p)}>
    <circle cx="12" cy="12" r="9" />
    <path d="m8 12.2 2.8 2.8L16.2 9.5" />
  </svg>
)

export const InfoIcon = (p: IconProps) => (
  <svg {...base(p)}>
    <circle cx="12" cy="12" r="9" />
    <path d="M12 11v5M12 8h.01" />
  </svg>
)

export const MessageIcon = (p: IconProps) => (
  <svg {...base(p)}>
    <path d="M20 12a8 8 0 0 1-11.8 7L4 20l1.1-4A8 8 0 1 1 20 12Z" />
  </svg>
)

export const PhoneIcon = (p: IconProps) => (
  <svg {...base(p)}>
    <path d="M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2Z" />
  </svg>
)

export const ChatIcon = (p: IconProps) => (
  <svg {...base(p)}>
    <path d="M7 18.5 3.5 21V6a2 2 0 0 1 2-2h13a2 2 0 0 1 2 2v10.5a2 2 0 0 1-2 2H7Z" />
    <path d="M8 9h8M8 13h5" />
  </svg>
)

export const PencilIcon = (p: IconProps) => (
  <svg {...base(p)}>
    <path d="M4 20h4L19 9a2.8 2.8 0 0 0-4-4L4 16v4Z" />
    <path d="m13.5 6.5 4 4" />
  </svg>
)

export const XIcon = (p: IconProps) => (
  <svg {...base({ size: 14, strokeWidth: 2.4, ...p })}>
    <path d="M6 6l12 12M18 6 6 18" />
  </svg>
)

export const CheckIcon = (p: IconProps) => (
  <svg {...base({ size: 18, strokeWidth: 2.6, ...p })}>
    <path d="m5 12.5 4.5 4.5L19 7.5" />
  </svg>
)

export const FlagIcon = (p: IconProps) => (
  <svg {...base(p)}>
    <path d="M5 21V4M5 4h11l-2 4 2 4H5" />
  </svg>
)

export const TrashIcon = (p: IconProps) => (
  <svg {...base(p)}>
    <path d="M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13" />
  </svg>
)

export const BrandMark = (p: IconProps) => (
  <svg {...base({ size: 14, strokeWidth: 2.4, ...p })}>
    <path d="M12 4 6 6.5v4.8c0 3.6 2.5 6.6 6 7.7 3.5-1.1 6-4.1 6-7.7V6.5L12 4Z" />
  </svg>
)
