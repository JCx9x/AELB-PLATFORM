import Image from 'next/image'

// public/logo-aelb.png: insignia oficial recortada con fondo transparente (602×640).
const RATIO = 602 / 640

export default function AelbLogo({ height, className, priority }: {
  height: number
  className?: string
  priority?: boolean
}) {
  return (
    <Image
      src="/logo-aelb.png"
      alt="AELB — Asociación Española de Lucha de Brazos"
      width={Math.round(height * RATIO)}
      height={height}
      className={className}
      priority={priority}
    />
  )
}
