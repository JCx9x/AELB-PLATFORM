import { Suspense } from 'react'
import ChampionshipDetailClient from './ChampionshipDetailClient'

export default function Page() {
  return (
    <Suspense>
      <ChampionshipDetailClient />
    </Suspense>
  )
}
