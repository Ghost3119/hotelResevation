import { spawnSync } from 'node:child_process'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const repoRoot = join(__dirname, '..', '..')
const performanceDir = join(repoRoot, 'performance')
const scriptPath = join(performanceDir, 'availability.k6.js')

for (const name of ['ADMIN_EMAIL', 'ADMIN_PASSWORD']) {
  if (!process.env[name]) {
    console.error(`[test:load] ${name} is required; no default credential is provided.`)
    process.exit(1)
  }
}

function run(command, args, options = {}) {
  return spawnSync(command, args, {
    stdio: 'inherit',
    shell: false,
    env: process.env,
    ...options,
  })
}

function commandExists(command, args) {
  const result = spawnSync(command, args, {
    stdio: 'ignore',
    shell: false,
  })
  return !result.error && result.status === 0
}

if (commandExists('k6', ['version'])) {
  const result = run('k6', ['run', scriptPath])
  process.exit(result.status ?? 1)
}

if (!commandExists('docker', ['version'])) {
  console.error('[test:load] k6 is not installed and Docker is not available.')
  process.exit(1)
}

const apiBaseUrl = process.env.API_BASE_URL || 'http://host.docker.internal:5173/api'
console.log('[test:load] k6 not found locally. Running grafana/k6 through Docker.')

const result = run('docker', [
  'run',
  '--rm',
  '-e',
  `API_BASE_URL=${apiBaseUrl}`,
  '-e',
  `ADMIN_EMAIL=${process.env.ADMIN_EMAIL}`,
  '-e',
  `ADMIN_PASSWORD=${process.env.ADMIN_PASSWORD}`,
  '-v',
  `${performanceDir}:/scripts:ro`,
  'grafana/k6',
  'run',
  '/scripts/availability.k6.js',
])

process.exit(result.status ?? 1)
