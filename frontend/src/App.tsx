import { useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'

const categories = [
  { key: 'dining', label: 'Dining & takeout' },
  { key: 'groceries', label: 'Eligible grocery stores' },
  { key: 'gas', label: 'Gas' },
  { key: 'travel', label: 'Travel' },
  { key: 'other', label: 'Other purchases' },
] as const

type Category = (typeof categories)[number]['key']

type Recommendation = {
  name: string
  rewardType: string
  annualFee: number
  annualRewards: number
  netAnnualValue: number
  explanation: string
}

type Results = {
  recommendations: Recommendation[]
  assumptions: string[]
}

const money = (value: number) =>
  value.toLocaleString('en-US', {
    style: 'currency',
    currency: 'USD',
  })

function App() {
  const [spending, setSpending] = useState<Record<Category, string>>({
    dining: '',
    groceries: '',
    gas: '',
    travel: '',
    other: '',
  })
  const [rewardPreference, setRewardPreference] = useState('cashback')
  const [maxAnnualFee, setMaxAnnualFee] = useState('0')
  const [results, setResults] = useState<Results | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const monthlyTotal = Object.values(spending).reduce(
    (total, amount) => total + Number(amount),
    0,
  )

  function clearResults() {
    setResults(null)
    setError('')
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoading(true)
    clearResults()

    try {
      const response = await fetch('/api/recommendations', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          spending: Object.fromEntries(
            Object.entries(spending).map(([key, value]) => [
              key,
              Number(value),
            ]),
          ),
          rewardPreference,
          maxAnnualFee: Number(maxAnnualFee),
        }),
      })

      if (!response.ok) {
        throw new Error(
          response.status === 400
            ? 'Please check your spending amounts and preferences.'
            : 'Could not calculate recommendations. Please try again.',
        )
      }

      const data: Results = await response.json()
      setResults(data)
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : 'Unable to reach the server. Please try again.',
      )
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="page">
      <header>
        <span className="brand">CardFit</span>
        <span className="badge">Independent portfolio project</span>
      </header>

      <section className="intro">
        <p className="eyebrow">CAPITAL ONE CARD COMPARISON</p>
        <h1>Find a card that fits your everyday.</h1>
        <p>
          Compare estimated annual reward value based on how you spend.
        </p>
      </section>

      <form onSubmit={handleSubmit} onChange={clearResults}>
        <fieldset disabled={loading} className="quiz-fields">
          <legend className="sr-only">
            Spending and reward preferences
          </legend>

          <section className="panel">
            <h2>What does a typical month look like?</h2>
            <p className="muted">
              Enter monthly card spending. Use 0 for categories you
              don’t spend in. Put Walmart and Target purchases under
              Other purchases.
            </p>

            <div className="fields">
              {categories.map(({ key, label }) => (
                <label key={key} htmlFor={key}>
                  {label}
                  <div className="money-input">
                    <span aria-hidden="true">$</span>
                    <input
                      id={key}
                      type="number"
                      min="0"
                      max="100000"
                      step="0.01"
                      placeholder="0"
                      required
                      value={spending[key]}
                      onChange={(event) =>
                        setSpending({
                          ...spending,
                          [key]: event.target.value,
                        })
                      }
                    />
                  </div>
                </label>
              ))}
            </div>

            <div className="total">
              Monthly spending
              <strong>{money(monthlyTotal)}</strong>
            </div>
          </section>

          <section className="panel">
            <h2>What matters to you?</h2>

            <div className="fields">
              <label htmlFor="rewards">
                Preferred rewards
                <select
                  id="rewards"
                  value={rewardPreference}
                  onChange={(event) =>
                    setRewardPreference(event.target.value)
                  }
                >
                  <option value="cashback">Cash back</option>
                  <option value="travel">Travel miles</option>
                  <option value="either">Either works for me</option>
                </select>
              </label>

              <label htmlFor="fee">
                Maximum annual fee
                <select
                  id="fee"
                  value={maxAnnualFee}
                  onChange={(event) =>
                    setMaxAnnualFee(event.target.value)
                  }
                >
                  <option value="0">$0 — no annual fee</option>
                  <option value="100">Up to $100</option>
                  <option value="400">Up to $400</option>
                </select>
              </label>
            </div>
          </section>

          <button type="submit" disabled={loading}>
            {loading ? 'Comparing cards…' : 'Find my matches →'}
          </button>
        </fieldset>
      </form>

      <p className="sr-only" role="status">
        {loading
          ? 'Calculating recommendations.'
          : results
            ? `${results.recommendations.length} matching cards found.`
            : ''}
      </p>

      {error && (
        <p className="error-message" role="alert">
          {error}
        </p>
      )}

      {results && (
        <section className="results" aria-labelledby="results-heading">
          <h2 id="results-heading">Your card matches</h2>
          <p className="muted">
            Ranked by estimated annual reward value minus the annual fee,
            within your selected preferences.
          </p>

          {results.recommendations.length === 0 && (
            <p>No cards match these preferences. Try adjusting your options.</p>
          )}

          <div className="result-grid">
            {results.recommendations.map((card, index) => (
              <article className="panel result-card" key={card.name}>
                <span className="badge">
                  {index === 0 ? 'Highest estimated value' : `Match ${index + 1}`}
                </span>

                <h3>{card.name}</h3>

                <p className="reward-label">
                  {card.rewardType === 'cashback'
                    ? 'Cash back'
                    : 'Travel miles · valued at 1¢ each'}
                </p>

                <div className="annual-value">
                  {money(card.netAnnualValue)}
                </div>
                <p className="muted">Estimated yearly value after fee</p>

                <dl className="breakdown">
                  <div>
                    <dt>Annual reward value</dt>
                    <dd>{money(card.annualRewards)}</dd>
                  </div>
                  <div>
                    <dt>Annual fee</dt>
                    <dd>{money(card.annualFee)}</dd>
                  </div>
                </dl>

                <p className="explanation">{card.explanation}</p>
              </article>
            ))}
          </div>

          <details className="panel assumptions">
            <summary>How these estimates work</summary>
            <ul>
              {results.assumptions.map((assumption) => (
                <li key={assumption}>{assumption}</li>
              ))}
            </ul>
          </details>
        </section>
      )}

      <footer>
        Not affiliated with Capital One. Estimates exclude bonuses,
        special booking rates and perks. This tool does not determine
        credit approval.
      </footer>
    </main>
  )
}

export default App