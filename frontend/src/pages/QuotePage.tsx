import { useState, type FormEvent } from 'react'
import { PageHeader } from '../components/PageHeader'
import { LoadingState } from '../components/LoadingState'
import { EmptyState } from '../components/EmptyState'
import { Input, Select } from '../components/FormField'
import { useQuote } from '../hooks/useQuote'
import { useRoomTypes } from '../hooks/useRoomTypes'
import { useRatePlans } from '../hooks/useRatePlans'
import type { QuoteNightlyRateDto, QuoteResultDto } from '../api/generated/schema'
import { formatCurrency, formatDate, todayISO } from '../utils/format'
import {
  BTN_PRIMARY,
  CARD,
  CARD_BODY,
  DATA_TABLE,
  FORM_ALERT_ERROR,
  FORM_ROW,
  TABLE_TD,
  TABLE_TH,
  TABLE_EMPTY_TD,
} from '../utils/constants'

export function QuotePage() {
  const [checkIn, setCheckIn] = useState('')
  const [checkOut, setCheckOut] = useState('')
  const [roomTypeId, setRoomTypeId] = useState<number | ''>('')
  const [adults, setAdults] = useState(1)
  const [children, setChildren] = useState(0)
  const [ratePlanId, setRatePlanId] = useState<number | ''>('')
  const [promotionCode, setPromotionCode] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [result, setResult] = useState<QuoteResultDto | null>(null)

  const roomTypesQ = useRoomTypes({})
  const ratePlansQ = useRatePlans({ size: 100 })
  const quoteMut = useQuote()

  const roomTypes = roomTypesQ.data ?? []
  const ratePlans = (ratePlansQ.data?.content ?? []).filter(
    (rp) => !roomTypeId || rp.roomTypeId === Number(roomTypeId),
  )

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setResult(null)
    if (!checkIn || !checkOut) {
      setError('Indique las fechas de entrada y salida.')
      return
    }
    if (checkOut <= checkIn) {
      setError('La fecha de salida debe ser posterior a la de entrada.')
      return
    }
    if (!roomTypeId) {
      setError('Seleccione un tipo de habitación.')
      return
    }
    try {
      const res = await quoteMut.mutateAsync({
        checkIn,
        checkOut,
        roomTypeId: Number(roomTypeId),
        adults,
        children,
        ratePlanId: ratePlanId ? Number(ratePlanId) : null,
        promotionCode: promotionCode.trim() || null,
      })
      setResult(res)
    } catch (err) {
      const msg =
        err && typeof err === 'object' && 'message' in err
          ? (err as { message: string }).message
          : 'No se pudo generar la cotización.'
      setError(msg)
    }
  }

  return (
    <div>
      <PageHeader
        title="Cotizador"
        subtitle="Cotización informativa con desglose por noche, impuestos y cargos (MXN)"
      />

      <form className={`${CARD} ${CARD_BODY} mb-4`} onSubmit={onSubmit} noValidate>
        {error && <div className={FORM_ALERT_ERROR} role="alert">{error}</div>}
        <div className={FORM_ROW}>
          <Input
            label="Entrada"
            name="checkIn"
            type="date"
            value={checkIn}
            min={todayISO()}
            onChange={(e) => setCheckIn(e.target.value)}
            required
          />
          <Input
            label="Salida"
            name="checkOut"
            type="date"
            value={checkOut}
            min={checkIn || todayISO()}
            onChange={(e) => setCheckOut(e.target.value)}
            required
          />
          <Select
            label="Tipo de habitación"
            name="roomTypeId"
            value={roomTypeId}
            onChange={(e) => {
              setRoomTypeId(e.target.value ? Number(e.target.value) : '')
              setRatePlanId('')
            }}
            required
          >
            <option value="">Seleccione…</option>
            {roomTypes.filter((rt) => rt.active).map((rt) => (
              <option key={rt.id} value={rt.id}>{rt.name}</option>
            ))}
          </Select>
          <Input
            label="Adultos"
            name="adults"
            type="number"
            min={1}
            value={adults}
            onChange={(e) => setAdults(Number(e.target.value))}
            required
          />
          <Input
            label="Niños"
            name="children"
            type="number"
            min={0}
            value={children}
            onChange={(e) => setChildren(Number(e.target.value))}
          />
          <Select
            label="Plan tarifario"
            name="ratePlanId"
            value={ratePlanId}
            onChange={(e) => setRatePlanId(e.target.value ? Number(e.target.value) : '')}
            hint="Vacío = plan por defecto"
          >
            <option value="">Por defecto</option>
            {ratePlans.map((rp) => (
              <option key={rp.id} value={rp.id}>{rp.name}</option>
            ))}
          </Select>
          <Input
            label="Código de promoción"
            name="promotionCode"
            value={promotionCode}
            onChange={(e) => setPromotionCode(e.target.value)}
            hint="Opcional"
          />
        </div>
        <div className="mt-2 flex justify-end">
          <button type="submit" className={BTN_PRIMARY} disabled={quoteMut.isPending}>
            {quoteMut.isPending ? 'Cotizando…' : 'Cotizar'}
          </button>
        </div>
      </form>

      {quoteMut.isPending && <LoadingState label="Generando cotización…" />}

      {!quoteMut.isPending && !result && !error && (
        <EmptyState
          title="Sin cotización"
          message="Complete el formulario y presione Cotizar para ver el desglose."
        />
      )}

      {result && <QuoteResult result={result} />}
    </div>
  )
}

function QuoteResult({ result }: { result: QuoteResultDto }) {
  return (
    <section aria-label="Desglose de cotización">
      <h3 className="mb-2.5 text-base font-semibold text-slate-900">Desglose por noche</h3>
      <div className={DATA_TABLE}>
        <div className="overflow-x-auto">
          <table className="w-full border-collapse">
            <thead>
              <tr>
                <th scope="col" className={TABLE_TH}>Fecha</th>
                <th scope="col" className={`${TABLE_TH} text-right`}>Tarifa base</th>
                <th scope="col" className={`${TABLE_TH} text-right`}>Cargo extra</th>
                <th scope="col" className={`${TABLE_TH} text-right`}>Descuento</th>
                <th scope="col" className={`${TABLE_TH} text-right`}>Impuestos</th>
                <th scope="col" className={`${TABLE_TH} text-right`}>Cargos</th>
                <th scope="col" className={`${TABLE_TH} text-right`}>Total noche</th>
              </tr>
            </thead>
            <tbody>
              {result.nightly.length === 0 ? (
                <tr>
                  <td className={TABLE_EMPTY_TD} colSpan={7}>Sin noches en el rango.</td>
                </tr>
              ) : (
                result.nightly.map((n: QuoteNightlyRateDto, i: number) => (
                  <tr key={`${n.date}-${i}`}>
                    <td className={TABLE_TD}>{formatDate(n.date)}</td>
                    <td className={`${TABLE_TD} text-right`}>{formatCurrency(n.baseRate)}</td>
                    <td className={`${TABLE_TD} text-right`}>{formatCurrency(n.extraPersonCharge)}</td>
                    <td className={`${TABLE_TD} text-right`}>{formatCurrency(n.discount)}</td>
                    <td className={`${TABLE_TD} text-right`}>{formatCurrency(n.taxes)}</td>
                    <td className={`${TABLE_TD} text-right`}>{formatCurrency(n.fees)}</td>
                    <td className={`${TABLE_TD} text-right font-semibold`}>{formatCurrency(n.total)}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <SummaryCard label="Subtotal" value={result.subtotal} />
        <SummaryCard label="Descuento total" value={result.totalDiscount} />
        <SummaryCard label="Impuestos" value={result.totalTaxes} />
        <SummaryCard label="Cargos" value={result.totalFees} />
      </div>

      <div className="mt-4 flex flex-wrap items-center justify-between gap-3 rounded-lg border border-blue-200 bg-blue-50 px-4 py-3">
        <span className="text-sm font-medium text-blue-900">Total general</span>
        <span className="text-2xl font-bold text-blue-900" data-testid="quote-grand-total">
          {formatCurrency(result.grandTotal)}
        </span>
      </div>
    </section>
  )
}

function SummaryCard({ label, value }: { label: string; value: number }) {
  return (
    <div className="flex flex-col gap-0.5 rounded-lg border border-slate-200 bg-white px-4 py-3 shadow-sm">
      <span className="text-xs uppercase tracking-wide text-slate-500">{label}</span>
      <span className="text-lg font-semibold text-slate-900">{formatCurrency(value)}</span>
    </div>
  )
}
