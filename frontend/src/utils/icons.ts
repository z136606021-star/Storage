import {
  BookOutlined,
  BulbOutlined,
  DatabaseOutlined,
  HomeOutlined,
  InboxOutlined,
  ProjectOutlined,
  SettingOutlined,
  ShoppingOutlined,
  ToolOutlined,
} from '@ant-design/icons-vue'
import type { Component } from 'vue'

const iconMap: Record<string, Component> = {
  BookOutlined,
  BulbOutlined,
  DatabaseOutlined,
  HomeOutlined,
  InboxOutlined,
  ProjectOutlined,
  SettingOutlined,
  ShoppingOutlined,
  ToolOutlined,
}

export function resolveIcon(name?: string | null): Component | undefined {
  if (!name) {
    return undefined
  }
  return iconMap[name]
}
