import * as Icons from '@ant-design/icons-vue'
import type { Component } from 'vue'

const iconMap = Icons as Record<string, Component>

export function resolveIcon(name?: string | null): Component | undefined {
  if (!name) {
    return undefined
  }
  return iconMap[name]
}
