import type { Component } from 'vue'

type RouteComponentLoader = () => Promise<Component>

const routeComponentModules = import.meta.glob([
  '../views/**/*.vue',
  '../components/**/*.vue',
]) as Record<string, RouteComponentLoader>

export function normalizeRouteComponentKey(componentKey?: string | null) {
  if (!componentKey) {
    return null
  }

  let value = componentKey.trim().replace(/\\/g, '/')
  if (!value) {
    return null
  }

  value = value
    .replace(/^@\/?/, '')
    .replace(/^frontend\/src\//, '')
    .replace(/^src\//, '')
    .replace(/^\//, '')

  if (!value.endsWith('.vue')) {
    value = `${value}.vue`
  }

  if (!value.startsWith('views/') && !value.startsWith('components/')) {
    return null
  }

  return `../${value}`
}

export function resolveRouteComponent(componentKey?: string | null) {
  const normalizedKey = normalizeRouteComponentKey(componentKey)
  if (!normalizedKey) {
    return null
  }
  return routeComponentModules[normalizedKey] ?? null
}
