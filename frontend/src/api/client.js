import axios from 'axios'
import { loadStoredAuth, logoutLocal, refreshAccessToken } from '../auth/keycloak'

const API_BASE = import.meta.env.VITE_API_BASE || ''

export const api = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const stored = loadStoredAuth()
  if (stored?.accessToken) {
    config.headers.Authorization = `Bearer ${stored.accessToken}`
  }
  config.headers['X-Request-Id'] = crypto.randomUUID()
  return config
})

let refreshing = null

api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config
    if (error.response?.status === 401 && original && !original._retry) {
      original._retry = true
      const stored = loadStoredAuth()
      if (!stored?.refreshToken) {
        logoutLocal()
        window.location.assign('/login')
        return Promise.reject(error)
      }
      try {
        refreshing = refreshing || refreshAccessToken(stored.refreshToken)
        const next = await refreshing
        refreshing = null
        original.headers.Authorization = `Bearer ${next.accessToken}`
        return api(original)
      } catch (e) {
        refreshing = null
        logoutLocal()
        window.location.assign('/login')
        return Promise.reject(e)
      }
    }
    return Promise.reject(error)
  },
)

export function getErrorMessage(err) {
  return (
    err?.response?.data?.message ||
    err?.response?.data?.code ||
    err?.message ||
    'Request failed'
  )
}

export function isNotFound(err) {
  return err?.response?.status === 404
}

export async function softGet(promise) {
  try {
    return await promise
  } catch (err) {
    if (isNotFound(err)) return null
    throw err
  }
}

/** Fetch every page from a Spring Page API (backend allows up to 500). */
export async function listAll(fetcher, pageSize = 200) {
  const all = []
  let page = 0
  let totalPages = 1
  const size = Math.min(pageSize, 500)
  while (page < totalPages) {
    const res = await fetcher(page, size)
    all.push(...(res?.content || []))
    totalPages = Math.max(1, res?.totalPages || 1)
    page += 1
    if (page > 100) break
  }
  return all
}
