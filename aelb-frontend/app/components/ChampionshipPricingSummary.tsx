type Props = { dark?: boolean }

/** Resumen visible de la política de precios WAF de un campeonato. */
export default function ChampionshipPricingSummary({ dark = false }: Props) {
  const textColor = dark ? 'text-white/75' : 'text-[var(--color-on-surface-variant)]'
  const borderColor = dark ? 'border-white/20' : 'border-[var(--color-outline-variant)]'

  return (
    <details className={`text-xs ${textColor}`}>
      <summary className="cursor-pointer font-bold underline underline-offset-4 decoration-dotted">
        Precios por categoría
      </summary>
      <div className={`mt-2 max-w-md border-l-2 pl-3 leading-relaxed ${borderColor}`}>
        Cada categoría tiene su tarifa por brazo. Con un brazo en dos categorías se aplican ambas tarifas.
        Si una categoría tiene ambos brazos y se añade otra, la segunda aplica el suplemento fijo de combinación.
      </div>
    </details>
  )
}
