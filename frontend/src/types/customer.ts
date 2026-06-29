export interface SysCustomer {
  id: number
  customerCode: string
  name: string
  contactName: string | null
  phone: string | null
  email: string | null
  address: string | null
  remark: string | null
  status: number
  createdAt?: string
  updatedAt?: string
}

export interface SysCustomerQuery {
  customerCode?: string
  name?: string
  contactName?: string
  ids?: number[]
  page?: number
  pageSize?: number
}

export interface SysCustomerSavePayload {
  customerCode: string
  name: string
  contactName?: string | null
  phone?: string | null
  email?: string | null
  address?: string | null
  remark?: string | null
  status: number
}

export type SysCustomerExportQuery = Omit<SysCustomerQuery, 'page' | 'pageSize'>
