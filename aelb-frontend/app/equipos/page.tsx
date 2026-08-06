'use client'

import React, { useState, useEffect, useRef } from 'react'
import { useTranslations } from 'next-intl'
import Navbar from '../components/Navbar'
import { getTeams, createTeam, updateTeam, deleteTeam, teamLogoUrl, type Team, type TeamData } from '../lib/api'
import { getSession } from '../lib/auth'

// ── Coordinate system ─────────────────────────────────────────────────────────
// x = (lon + 9.5) * 35 + 10
// y = (44.0 - lat) * 43.5 + 10
// viewBox: "0 0 530 465"

const PROVINCE_COORDS: Record<string, [number, number]> = {
  'A Coruña':      [49,  37],  'Lugo':         [78,  53],  'Ourense':      [67,  82],
  'Pontevedra':    [40,  78],  'Asturias':     [138, 38],  'Cantabria':    [203, 46],
  'Vizcaya':       [240, 42],  'Guipúzcoa':    [272, 40],  'Álava':        [249, 60],
  'Navarra':       [285, 61],  'La Rioja':     [257, 84],  'Burgos':       [213, 82],
  'Palencia':      [184, 97],  'León':         [148, 71],  'Zamora':       [142, 119],
  'Valladolid':    [177, 112], 'Salamanca':    [144, 142], 'Ávila':        [178, 155],
  'Segovia':       [198, 143], 'Soria':        [256, 107], 'Zaragoza':     [312, 112],
  'Huesca':        [328, 91],  'Lleida':       [364, 114], 'Girona':       [441, 98],
  'Barcelona':     [419, 124], 'Tarragona':    [386, 135], 'Madrid':       [213, 166],
  'Guadalajara':   [232, 157], 'Cuenca':       [268, 181], 'Teruel':       [304, 169],
  'Castellón':     [341, 184], 'Valencia':     [329, 207], 'Cáceres':      [120, 207],
  'Toledo':        [202, 190], 'Ciudad Real':  [205, 228], 'Badajoz':      [99,  233],
  'Albacete':      [277, 228], 'Murcia':       [303, 272], 'Alicante':     [326, 256],
  'Jaén':          [210, 281], 'Córdoba':      [175, 276], 'Granada':      [217, 307],
  'Sevilla':       [133, 298], 'Málaga':       [188, 327], 'Huelva':       [99,  303],
  'Almería':       [256, 322], 'Cádiz':        [122, 335],
  'Islas Baleares':[435, 203],
  // Canarias — posiciones dentro del recuadro inset
  'Las Palmas':              [101, 446],
  'Santa Cruz de Tenerife':  [67,  440],
}

// ── Contorno detallado de la Península Ibérica (88 puntos, horario) ───────────
const SPAIN_PATH = [
  'M 18,58',                                                    // Cabo Finisterre
  'L 20,49 L 34,39 L 49,33 L 51,29',                           // Costa da Morte → A Coruña → Cabo Prior
  'L 67,20 L 73,19',                                            // Cabo Ortegal → Estaca de Bares
  'L 82,23 L 96,30',                                            // San Ciprián → Ribadeo
  'L 124,27 L 138,25 L 144,30',                                 // Cabo Vidio → Cabo Peñas → Gijón
  'L 158,31 L 176,35 L 189,37',                                 // Lastres → Llanes → San Vicente B.
  'L 210,33 L 224,34 L 239,38',                                 // Santander → Laredo → Bilbao
  'L 264,40 L 273,40 L 280,37',                                 // Zumaia → San Sebastián → Hondarribia
  'L 296,51 L 317,64 L 364,63 L 410,78 L 443,79 L 453,78',    // Pirineos
  'L 458,83 L 453,85 L 452,97',                                 // Cap de Creus → Roses → L'Escala
  'L 440,111 L 428,117 L 419,124 L 406,130',                   // Blanes → Mataró → Barcelona → Sitges
  'L 383,138 L 368,152',                                        // Tarragona → Delta Ebro
  'L 353,172 L 342,184 L 336,198 L 331,207',                   // Alcossebre → Castellón → Sagunt → Valencia
  'L 334,221 L 346,234 L 351,239 L 344,243',                   // Cullera → Dénia → Cabo de la Nao → Calp
  'L 326,256 L 320,267 L 318,272 L 318,287',                   // Alicante → Guardamar → Torrevieja → Cabo de Palos
  'L 308,288 L 296,290 L 287,297 L 278,304',                   // Cartagena → Mazarrón → Águilas → Vera
  'L 266,327 L 256,321 L 237,326 L 219,326 L 188,327',         // Cabo de Gata → Almería → Adra → Motril → Málaga
  'L 162,339 L 152,352 L 146,358',                             // Estepona → Punta Paloma → Tarifa
  'L 131,350 L 117,326 L 103,309 L 99,307 L 84,305',          // Cabo Trafalgar → Chipiona → Huelva → Ayamonte
  'L 81,292 L 85,265 L 99,233',                                // Frontera Portugal sur-centro
  'L 84,211 L 82,199 L 93,184 L 103,159',                     // Marvão → Castelo de Vide → Cáceres frontera → Salamanca
  'L 120,119 L 106,107 L 81,109 L 56,100',                    // Miranda Douro → Bragança → Chaves → Lindoso
  'L 40,95 L 32,101',                                          // Tui/Valença → A Guarda
  'L 37,91 L 31,87 L 34,80 L 25,64 L 25,62 L 18,58 Z',       // Vigo → Cabo Home → Pontevedra → Corrubedo → Finisterre
].join(' ')

// ── Islas Baleares ────────────────────────────────────────────────────────────
const MALLORCA_PATH  = 'M 418,203 L 427,189 L 444,185 L 455,187 L 461,195 L 457,208 L 444,217 L 427,210 Z'
const MENORCA_PATH   = 'M 477,190 L 484,186 L 494,180 L 491,185 L 479,193 Z'
const IBIZA_PATH     = 'M 371,213 L 383,214 L 385,230 L 370,227 Z'
const FORMENTERA_PATH= 'M 372,234 L 381,233 L 380,237 L 371,238 Z'

// ── Recuadro Islas Canarias ───────────────────────────────────────────────────
// x_can(lon) = (lon + 18.3) / 5.3 * 173 + 12
// y_can(lat) = (29.9 - lat) / 2.5 * 57 + 403
const CAN_BOX = { x: 12, y: 403, w: 173, h: 57 }

// Cada isla: cx,cy en SVG, rx,ry en px, rotate en grados
const CANARY_ISLANDS = [
  { id: 'hierro',       cx: 22,  cy: 452, rx: 4,  ry: 4,  rot: 0  },
  { id: 'palma',        cx: 26,  cy: 431, rx: 5,  ry: 7,  rot: 0  },
  { id: 'gomera',       cx: 47,  cy: 444, rx: 4,  ry: 4,  rot: 0  },
  { id: 'tenerife',     cx: 67,  cy: 440, rx: 12, ry: 7,  rot: -10 },
  { id: 'gran-canaria', cx: 101, cy: 446, rx: 7,  ry: 7,  rot: 0  },
  { id: 'fuerteventura',cx: 153, cy: 438, rx: 4,  ry: 10, rot: 15 },
  { id: 'lanzarote',    cx: 165, cy: 423, rx: 4,  ry: 8,  rot: 10 },
]

type FormMode = 'create' | 'edit'

const EMPTY_FORM: TeamData = {
  name: '', responsibleName: '', responsibleLastName: '',
  phone: '', province: '', locality: '',
  logoBase64: null, logoType: null,
}

export default function EquiposPage() {
  const t = useTranslations('Equipos')
  const [teams,            setTeams]            = useState<Team[]>([])
  const [loading,          setLoading]          = useState(true)
  const [error,            setError]            = useState<string | null>(null)
  const [selected,         setSelected]         = useState<Team | null>(null)
  const [selectedProvince, setSelectedProvince] = useState<string | null>(null)
  const [hoveredProvince,  setHoveredProvince]  = useState<string | null>(null)

  const [showForm,  setShowForm]  = useState(false)
  const [formMode,  setFormMode]  = useState<FormMode>('create')
  const [formData,  setFormData]  = useState<TeamData>(EMPTY_FORM)
  const [editId,    setEditId]    = useState<string | null>(null)
  const [saving,    setSaving]    = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const fileRef = useRef<HTMLInputElement>(null)

  const session   = typeof window !== 'undefined' ? getSession() : null
  const isManager = session?.role === 'GESTOR' || session?.role === 'ADMIN'

  useEffect(() => { loadTeams() }, [])

  function loadTeams() {
    setLoading(true)
    getTeams()
      .then(setTeams)
      .catch((err: { detail?: string }) => setError(err.detail ?? t('errorCarga')))
      .finally(() => setLoading(false))
  }

  function openCreate() {
    setFormData(EMPTY_FORM)
    setEditId(null)
    setFormMode('create')
    setFormError(null)
    setShowForm(true)
  }

  function openEdit(team: Team) {
    setFormData({
      name:                team.name,
      responsibleName:     team.responsibleName,
      responsibleLastName: team.responsibleLastName,
      phone:               team.phone ?? '',
      province:            team.province,
      locality:            team.locality,
      logoBase64:          null,
      logoType:            null,
    })
    setEditId(team.id)
    setFormMode('edit')
    setFormError(null)
    setShowForm(true)
    setSelected(null)
  }

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return
    const reader = new FileReader()
    reader.onload = (ev) => {
      const result = ev.target?.result as string
      // result is "data:image/png;base64,xxxx" — strip prefix
      const base64 = result.split(',')[1]
      setFormData(d => ({ ...d, logoBase64: base64, logoType: file.type }))
    }
    reader.readAsDataURL(file)
  }

  async function handleDelete(id: string) {
    if (!confirm(t('confirmarEliminar'))) return
    try {
      await deleteTeam(id)
      setSelected(null)
      setSelectedProvince(null)
      loadTeams()
    } catch (err: unknown) {
      const e = err as { detail?: string }
      alert(e.detail ?? t('errorEliminar'))
    }
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setSaving(true)
    setFormError(null)
    try {
      if (formMode === 'create') {
        await createTeam(formData)
      } else if (editId) {
        await updateTeam(editId, formData)
      }
      setShowForm(false)
      if (fileRef.current) fileRef.current.value = ''
      loadTeams()
    } catch (err: unknown) {
      const e = err as { detail?: string }
      setFormError(e.detail ?? t('errorGuardar'))
    } finally {
      setSaving(false)
    }
  }

  function getCoords(province: string): [number, number] | null {
    return PROVINCE_COORDS[province] ?? null
  }

  // Group teams by province for cluster pins
  const teamsByProvince = teams.reduce<Record<string, Team[]>>((acc, t) => {
    if (!acc[t.province]) acc[t.province] = []
    acc[t.province].push(t)
    return acc
  }, {})

  return (
    <div style={{ backgroundColor: 'var(--color-surface)', minHeight: '100vh' }}>
      <Navbar />

      <div className="max-w-7xl mx-auto px-4 lg:px-8 py-8 lg:py-12">

        {/* Header */}
        <div className="flex items-start justify-between mb-8 gap-4 flex-wrap">
          <div>
            <h1 className="font-extrabold text-3xl lg:text-4xl"
              style={{ color: 'var(--color-on-surface)', letterSpacing: '-0.03em' }}>
              {t('titulo')}
            </h1>
            <p className="mt-2 text-sm lg:text-base" style={{ color: 'var(--color-on-surface-variant)' }}>
              {t('subtitulo')}
            </p>
          </div>
          {isManager && (
            <button onClick={openCreate}
              className="flex items-center gap-2 px-4 py-2 rounded-[0.25rem] text-sm font-bold text-white transition-opacity hover:opacity-90"
              style={{ backgroundColor: 'var(--color-primary-cta)' }}>
              <PlusIcon className="w-4 h-4" /> {t('anadirEquipo')}
            </button>
          )}
        </div>

        {loading && (
          <div className="flex items-center justify-center py-24">
            <div className="w-8 h-8 border-4 border-[#d32f2f] border-t-transparent rounded-full animate-spin" />
          </div>
        )}

        {error && (
          <div className="flex items-center gap-3 px-4 py-3 rounded-[0.25rem] text-sm mb-6"
            style={{ backgroundColor: '#fef2f2', color: 'var(--color-error)' }}>
            {error}
          </div>
        )}

        {!loading && !error && (
          <div className="flex flex-col xl:flex-row gap-8">

            {/* ── SVG Map ── */}
            <div className="flex-1 min-w-0">
              <div className="rounded-[0.25rem] border overflow-hidden"
                style={{ backgroundColor: 'var(--color-surface-card)', borderColor: 'var(--color-outline-variant)' }}>
                <div className="px-4 py-3 border-b text-sm font-semibold"
                  style={{ borderColor: 'var(--color-outline-variant)', color: 'var(--color-on-surface-variant)' }}>
                  {t('mapaTitulo', { count: teams.length })}
                </div>

                <div className="p-4 overflow-x-auto">
                  <svg viewBox="0 0 530 465" className="w-full max-w-2xl mx-auto" style={{ minWidth: 300 }}>
                    <defs>
                      <filter id="shadow">
                        <feDropShadow dx="0" dy="1" stdDeviation="1" floodOpacity="0.18" />
                      </filter>
                    </defs>

                    {/* Península */}
                    <path d={SPAIN_PATH} fill="#e8edf2" stroke="#a0aec0" strokeWidth="1" strokeLinejoin="round" />

                    {/* Islas Baleares */}
                    <path d={MALLORCA_PATH}   fill="#e8edf2" stroke="#a0aec0" strokeWidth="1" strokeLinejoin="round" />
                    <path d={MENORCA_PATH}    fill="#e8edf2" stroke="#a0aec0" strokeWidth="1" strokeLinejoin="round" />
                    <path d={IBIZA_PATH}      fill="#e8edf2" stroke="#a0aec0" strokeWidth="1" strokeLinejoin="round" />
                    <path d={FORMENTERA_PATH} fill="#e8edf2" stroke="#a0aec0" strokeWidth="0.8" strokeLinejoin="round" />

                    {/* Ceuta */}
                    <circle cx="156" cy="363" r="3" fill="#e8edf2" stroke="#a0aec0" strokeWidth="1" />
                    <text x="160" y="367" fontSize="5.5" fill="#718096">CE</text>

                    {/* Melilla */}
                    <circle cx="239" cy="389" r="3" fill="#e8edf2" stroke="#a0aec0" strokeWidth="1" />
                    <text x="243" y="393" fontSize="5.5" fill="#718096">ME</text>

                    {/* Recuadro Islas Canarias */}
                    <rect x={CAN_BOX.x} y={CAN_BOX.y} width={CAN_BOX.w} height={CAN_BOX.h}
                      fill="#f0f4f8" stroke="#a0aec0" strokeWidth="0.8" rx="2" />
                    <text x={CAN_BOX.x + CAN_BOX.w / 2} y={CAN_BOX.y - 2}
                      fontSize="6" fill="#718096" textAnchor="middle">{t('islasCanarias')}</text>

                    {/* Islas individuales en el recuadro */}
                    {CANARY_ISLANDS.map(isl => (
                      <ellipse key={isl.id}
                        cx={isl.cx} cy={isl.cy} rx={isl.rx} ry={isl.ry}
                        fill="#e8edf2" stroke="#a0aec0" strokeWidth="0.8"
                        transform={isl.rot ? `rotate(${isl.rot} ${isl.cx} ${isl.cy})` : undefined}
                      />
                    ))}

                    {/* Pins de equipos agrupados por provincia */}
                    {Object.entries(teamsByProvince).map(([province, provTeams]) => {
                      const coords  = getCoords(province)
                      if (!coords) return null

                      const count   = provTeams.length
                      const isMulti = count > 1
                      const isHov   = hoveredProvince === province
                      const hasSelected = selected && provTeams.some(t => t.id === selected.id)
                      const isProvinceSelected = selectedProvince === province

                      const r    = isMulti ? (isHov || isProvinceSelected ? 12 : 10)
                                           : (isHov || hasSelected ? 8 : 6)
                      const fill = (hasSelected || isProvinceSelected)
                                    ? 'var(--color-primary)'
                                    : '#d32f2f'

                      // Tooltip label
                      const tipLabel = isMulti
                        ? `${t('equipoCount', { count })} · ${province}`
                        : provTeams[0].name

                      const tipWidth = Math.min(Math.max(tipLabel.length * 5 + 16, 60), 160)

                      return (
                        <g key={province}
                          onClick={() => {
                            if (isMulti) {
                              setSelectedProvince(isProvinceSelected ? null : province)
                              setSelected(null)
                            } else {
                              const team = provTeams[0]
                              setSelected(hasSelected ? null : team)
                              setSelectedProvince(null)
                            }
                          }}
                          onMouseEnter={() => setHoveredProvince(province)}
                          onMouseLeave={() => setHoveredProvince(null)}
                          style={{ cursor: 'pointer' }}>

                          {/* Aura pulsante para clusters */}
                          {isMulti && (
                            <circle cx={coords[0]} cy={coords[1]} r={r + 4}
                              fill={fill} opacity="0.18" />
                          )}

                          <circle cx={coords[0]} cy={coords[1]} r={r}
                            fill={fill} stroke="white" strokeWidth="1.5" />

                          {/* Número en clusters */}
                          {isMulti && (
                            <text x={coords[0]} y={coords[1] + 3.5}
                              fontSize="8" fontWeight="700" fill="white" textAnchor="middle"
                              style={{ pointerEvents: 'none' }}>
                              {count}
                            </text>
                          )}

                          {/* Tooltip on hover */}
                          {isHov && (
                            <g>
                              <rect
                                x={coords[0] + 12} y={coords[1] - 20}
                                width={tipWidth} height={18}
                                fill="white" stroke="#e2e8f0" strokeWidth="1"
                                rx="3" filter="url(#shadow)" />
                              <text x={coords[0] + 18} y={coords[1] - 7}
                                fontSize="8" fontWeight="600" fill="#1a202c">
                                {tipLabel.length > 26 ? tipLabel.slice(0, 25) + '…' : tipLabel}
                              </text>
                            </g>
                          )}
                        </g>
                      )
                    })}
                  </svg>

                  {teams.length === 0 && (
                    <p className="text-center text-sm py-4" style={{ color: 'var(--color-on-surface-variant)' }}>
                      {t('sinEquipos')}
                    </p>
                  )}
                </div>
              </div>
            </div>

            {/* ── Columna derecha ── */}
            <div className="w-full xl:w-80 flex flex-col gap-4 flex-shrink-0">

              {/* Cluster — varios equipos de una misma provincia */}
              {selectedProvince && !selected && (
                <div className="rounded-[0.25rem] border overflow-hidden"
                  style={{ backgroundColor: 'var(--color-surface-card)', borderColor: 'var(--color-primary-cta)', borderWidth: 2 }}>
                  <div className="flex items-center justify-between px-4 py-3 border-b"
                    style={{ borderColor: 'var(--color-outline-variant)', backgroundColor: 'var(--color-surface)' }}>
                    <div>
                      <p className="text-xs font-bold uppercase tracking-widest"
                        style={{ color: 'var(--color-primary-cta)' }}>
                        {selectedProvince}
                      </p>
                      <p className="text-sm font-semibold mt-0.5" style={{ color: 'var(--color-on-surface)' }}>
                        {t('equipoCount', { count: teamsByProvince[selectedProvince]?.length ?? 0 })}
                      </p>
                    </div>
                    <button onClick={() => setSelectedProvince(null)} className="text-xs"
                      style={{ color: 'var(--color-on-surface-variant)' }}>✕</button>
                  </div>
                  <ul className="divide-y" style={{ borderColor: 'var(--color-outline-variant)' }}>
                    {(teamsByProvince[selectedProvince] ?? []).map(team => (
                      <li key={team.id}>
                        <button onClick={() => { setSelected(team); setSelectedProvince(null) }}
                          className="w-full text-left px-4 py-3 transition-colors hover:bg-[var(--color-surface)]">
                          <div className="flex items-center gap-3">
                            {team.hasLogo ? (
                              <img src={teamLogoUrl(team.id)} alt={team.name}
                                className="w-7 h-7 rounded-[0.25rem] object-cover flex-shrink-0 border"
                                style={{ borderColor: 'var(--color-outline-variant)' }} />
                            ) : (
                              <span className="w-7 h-7 rounded-[0.25rem] flex-shrink-0 flex items-center justify-center text-[9px] font-bold text-white"
                                style={{ backgroundColor: '#d32f2f' }}>
                                {team.name.slice(0, 2).toUpperCase()}
                              </span>
                            )}
                            <div>
                              <p className="text-sm font-semibold leading-tight"
                                style={{ color: 'var(--color-on-surface)' }}>
                                {team.name}
                              </p>
                              <p className="text-xs mt-0.5" style={{ color: 'var(--color-on-surface-variant)' }}>
                                {team.locality}
                              </p>
                            </div>
                          </div>
                        </button>
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {/* Detalle del equipo seleccionado */}
              {selected && (
                <div className="rounded-[0.25rem] border p-5"
                  style={{ backgroundColor: 'var(--color-surface-card)', borderColor: 'var(--color-primary-cta)', borderWidth: 2 }}>

                  <div className="flex items-start gap-3 mb-3">
                    {selected.hasLogo && (
                      <img src={teamLogoUrl(selected.id)} alt={selected.name}
                        className="w-12 h-12 rounded-[0.25rem] object-cover flex-shrink-0 border"
                        style={{ borderColor: 'var(--color-outline-variant)' }} />
                    )}
                    <div className="flex-1 min-w-0">
                      <div className="flex items-start justify-between">
                        <h2 className="font-bold text-base leading-tight"
                          style={{ color: 'var(--color-on-surface)' }}>
                          {selected.name}
                        </h2>
                        <button
                          onClick={() => {
                            // If this team's province has multiple teams, go back to the cluster view
                            const sameProvince = teamsByProvince[selected.province] ?? []
                            if (sameProvince.length > 1) {
                              setSelectedProvince(selected.province)
                            }
                            setSelected(null)
                          }}
                          className="text-xs ml-2 flex-shrink-0"
                          style={{ color: 'var(--color-on-surface-variant)' }}>✕</button>
                      </div>
                    </div>
                  </div>

                  <dl className="space-y-1.5 text-sm">
                    <TeamDetail label={t('campoResponsable')}  value={`${selected.responsibleName} ${selected.responsibleLastName}`} />
                    {selected.phone && <TeamDetail label={t('campoTelefono')} value={selected.phone} />}
                    <TeamDetail label={t('campoProvincia')}   value={selected.province} />
                    <TeamDetail label={t('campoLocalidad')}   value={selected.locality} />
                  </dl>

                  {isManager && (
                    <div className="flex gap-2 mt-4">
                      <button onClick={() => openEdit(selected)}
                        className="flex-1 py-1.5 text-xs font-bold rounded-[0.25rem] border transition-colors hover:bg-[var(--color-surface)]"
                        style={{ borderColor: 'var(--color-outline-variant)', color: 'var(--color-on-surface)' }}>
                        {t('editar')}
                      </button>
                      <button onClick={() => handleDelete(selected.id)}
                        className="flex-1 py-1.5 text-xs font-bold rounded-[0.25rem] border"
                        style={{ borderColor: '#fca5a5', color: 'var(--color-error)', backgroundColor: '#fff1f1' }}>
                        {t('eliminar')}
                      </button>
                    </div>
                  )}
                </div>
              )}

              {/* Lista de equipos */}
              {teams.length > 0 && (
                <div className="rounded-[0.25rem] border overflow-hidden"
                  style={{ backgroundColor: 'var(--color-surface-card)', borderColor: 'var(--color-outline-variant)' }}>
                  <div className="px-4 py-3 border-b text-xs font-bold uppercase tracking-widest"
                    style={{ borderColor: 'var(--color-outline-variant)', color: 'var(--color-on-surface-variant)' }}>
                    {t('listaEquipos')}
                  </div>
                  <ul className="divide-y" style={{ borderColor: 'var(--color-outline-variant)' }}>
                    {teams.map(team => (
                      <li key={team.id}>
                        <button onClick={() => setSelected(selected?.id === team.id ? null : team)}
                          className="w-full text-left px-4 py-3 hover:bg-[var(--color-surface)] transition-colors"
                          style={{ backgroundColor: selected?.id === team.id ? 'var(--color-surface)' : undefined }}>
                          <div className="flex items-center gap-3">
                            {team.hasLogo ? (
                              <img src={teamLogoUrl(team.id)} alt={team.name}
                                className="w-7 h-7 rounded-[0.25rem] object-cover flex-shrink-0 border"
                                style={{ borderColor: 'var(--color-outline-variant)' }} />
                            ) : (
                              <span className="w-7 h-7 rounded-[0.25rem] flex-shrink-0 flex items-center justify-center text-[9px] font-bold text-white"
                                style={{ backgroundColor: '#d32f2f' }}>
                                {team.name.slice(0, 2).toUpperCase()}
                              </span>
                            )}
                            <div>
                              <p className="text-sm font-semibold leading-tight"
                                style={{ color: 'var(--color-on-surface)' }}>
                                {team.name}
                              </p>
                              <p className="text-xs mt-0.5" style={{ color: 'var(--color-on-surface-variant)' }}>
                                {team.province} · {team.locality}
                              </p>
                            </div>
                          </div>
                        </button>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          </div>
        )}

        {/* ── Modal formulario ── */}
        {showForm && isManager && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4"
            style={{ backgroundColor: 'rgba(0,0,0,0.4)' }}>
            <div className="rounded-[0.25rem] w-full max-w-lg overflow-y-auto max-h-[90vh]"
              style={{ backgroundColor: 'var(--color-surface-card)' }}>

              <div className="flex items-center justify-between px-6 py-4 border-b"
                style={{ borderColor: 'var(--color-outline-variant)' }}>
                <h2 className="font-bold text-base" style={{ color: 'var(--color-on-surface)' }}>
                  {formMode === 'create' ? t('anadirEquipo') : t('editarEquipo')}
                </h2>
                <button onClick={() => setShowForm(false)} className="text-xl leading-none hover:opacity-70"
                  style={{ color: 'var(--color-on-surface-variant)' }}>✕</button>
              </div>

              <form onSubmit={handleSubmit} className="p-6 space-y-4">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">

                  <FormField label={t('campoNombreEquipo')} className="sm:col-span-2">
                    <input required maxLength={100} value={formData.name}
                      onChange={e => setFormData(d => ({ ...d, name: e.target.value }))}
                      className="w-full border rounded-[0.25rem] px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#d32f2f]"
                      style={{ borderColor: 'var(--color-outline-variant)' }} />
                  </FormField>

                  <FormField label={t('campoNombreResponsable')}>
                    <input required maxLength={100} value={formData.responsibleName}
                      onChange={e => setFormData(d => ({ ...d, responsibleName: e.target.value }))}
                      className="w-full border rounded-[0.25rem] px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#d32f2f]"
                      style={{ borderColor: 'var(--color-outline-variant)' }} />
                  </FormField>

                  <FormField label={t('campoApellidosResponsable')}>
                    <input required maxLength={100} value={formData.responsibleLastName}
                      onChange={e => setFormData(d => ({ ...d, responsibleLastName: e.target.value }))}
                      className="w-full border rounded-[0.25rem] px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#d32f2f]"
                      style={{ borderColor: 'var(--color-outline-variant)' }} />
                  </FormField>

                  <FormField label={t('campoTelefonoContacto')}>
                    <input maxLength={20} value={formData.phone ?? ''}
                      onChange={e => setFormData(d => ({ ...d, phone: e.target.value || null }))}
                      className="w-full border rounded-[0.25rem] px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#d32f2f]"
                      style={{ borderColor: 'var(--color-outline-variant)' }} />
                  </FormField>

                  <FormField label={t('campoProvinciaRequerida')}>
                    <select required value={formData.province}
                      onChange={e => setFormData(d => ({ ...d, province: e.target.value }))}
                      className="w-full border rounded-[0.25rem] px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#d32f2f] bg-white"
                      style={{ borderColor: 'var(--color-outline-variant)' }}>
                      <option value="">{t('seleccionar')}</option>
                      {PROVINCES.map(p => <option key={p} value={p}>{p}</option>)}
                    </select>
                  </FormField>

                  <FormField label={t('campoLocalidadRequerida')}>
                    <input required maxLength={100} value={formData.locality}
                      onChange={e => setFormData(d => ({ ...d, locality: e.target.value }))}
                      className="w-full border rounded-[0.25rem] px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#d32f2f]"
                      style={{ borderColor: 'var(--color-outline-variant)' }} />
                  </FormField>

                  {/* Logo upload */}
                  <FormField label={t('campoLogo')} className="sm:col-span-2">
                    <div className="flex items-center gap-3">
                      <label className="flex-1 flex items-center gap-2 cursor-pointer border rounded-[0.25rem] px-3 py-2 text-sm hover:bg-[var(--color-surface)] transition-colors"
                        style={{ borderColor: 'var(--color-outline-variant)', color: 'var(--color-on-surface-variant)' }}>
                        <ImageIcon className="w-4 h-4 flex-shrink-0" />
                        <span className="truncate">
                          {formData.logoBase64 ? t('imagenSeleccionada') : t('seleccionarImagen')}
                        </span>
                        <input ref={fileRef} type="file" accept="image/png,image/jpeg,image/jpg"
                          className="sr-only" onChange={handleFileChange} />
                      </label>
                      {formData.logoBase64 && (
                        <button type="button"
                          onClick={() => { setFormData(d => ({ ...d, logoBase64: null, logoType: null })); if (fileRef.current) fileRef.current.value = '' }}
                          className="text-xs px-2 py-2 rounded-[0.25rem] border"
                          style={{ borderColor: '#fca5a5', color: 'var(--color-error)' }}>
                          {t('quitar')}
                        </button>
                      )}
                    </div>
                    {formData.logoBase64 && (
                      <img src={`data:${formData.logoType};base64,${formData.logoBase64}`}
                        alt="Preview" className="mt-2 h-16 w-16 object-cover rounded-[0.25rem] border"
                        style={{ borderColor: 'var(--color-outline-variant)' }} />
                    )}
                    {formMode === 'edit' && !formData.logoBase64 && (
                      <p className="text-xs mt-1" style={{ color: 'var(--color-on-surface-variant)' }}>
                        {t('logoActualHint')}
                      </p>
                    )}
                  </FormField>
                </div>

                {formError && (
                  <p className="text-sm px-3 py-2 rounded-[0.25rem]"
                    style={{ backgroundColor: '#fef2f2', color: 'var(--color-error)' }}>
                    {formError}
                  </p>
                )}

                <div className="flex gap-3 pt-2">
                  <button type="button" onClick={() => setShowForm(false)}
                    className="flex-1 py-2 text-sm font-semibold rounded-[0.25rem] border transition-colors hover:bg-[var(--color-surface)]"
                    style={{ borderColor: 'var(--color-outline-variant)', color: 'var(--color-on-surface)' }}>
                    {t('cancelar')}
                  </button>
                  <button type="submit" disabled={saving}
                    className="flex-1 py-2 text-sm font-bold rounded-[0.25rem] text-white transition-opacity hover:opacity-90 disabled:opacity-60"
                    style={{ backgroundColor: 'var(--color-primary-cta)' }}>
                    {saving ? t('guardando') : formMode === 'create' ? t('crearEquipo') : t('guardarCambios')}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

// ── Sub-components ────────────────────────────────────────────────────────────

function TeamDetail({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex gap-2">
      <dt className="font-semibold flex-shrink-0" style={{ color: 'var(--color-on-surface-variant)', minWidth: 80 }}>
        {label}
      </dt>
      <dd style={{ color: 'var(--color-on-surface)' }}>{value}</dd>
    </div>
  )
}

function FormField({ label, children, className = '' }: {
  label: string; children: React.ReactNode; className?: string
}) {
  return (
    <div className={className}>
      <label className="block text-xs font-semibold mb-1" style={{ color: 'var(--color-on-surface-variant)' }}>
        {label}
      </label>
      {children}
    </div>
  )
}

function PlusIcon({ className }: { className?: string }) {
  return <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}><path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" /></svg>
}

function ImageIcon({ className }: { className?: string }) {
  return <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}><path strokeLinecap="round" strokeLinejoin="round" d="M2.25 15.75l5.159-5.159a2.25 2.25 0 013.182 0l5.159 5.159m-1.5-1.5l1.409-1.409a2.25 2.25 0 013.182 0l2.909 2.909m-18 3.75h16.5a1.5 1.5 0 001.5-1.5V6a1.5 1.5 0 00-1.5-1.5H3.75A1.5 1.5 0 002.25 6v12a1.5 1.5 0 001.5 1.5zm10.5-11.25h.008v.008h-.008V8.25zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0z" /></svg>
}

// ── Lista de provincias ───────────────────────────────────────────────────────
const PROVINCES = [
  'A Coruña', 'Álava', 'Albacete', 'Alicante', 'Almería', 'Asturias', 'Ávila',
  'Badajoz', 'Barcelona', 'Burgos', 'Cáceres', 'Cádiz', 'Cantabria', 'Castellón',
  'Ciudad Real', 'Córdoba', 'Cuenca', 'Girona', 'Granada', 'Guadalajara',
  'Guipúzcoa', 'Huelva', 'Huesca', 'Islas Baleares', 'Jaén', 'La Rioja',
  'Las Palmas', 'León', 'Lleida', 'Lugo', 'Madrid', 'Málaga', 'Murcia',
  'Navarra', 'Ourense', 'Palencia', 'Pontevedra', 'Salamanca',
  'Santa Cruz de Tenerife', 'Segovia', 'Sevilla', 'Soria', 'Tarragona',
  'Teruel', 'Toledo', 'Valencia', 'Valladolid', 'Vizcaya', 'Zamora', 'Zaragoza',
]
