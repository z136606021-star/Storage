export type FrontendErrorCode =
  | 'VUE_RUNTIME_ERROR'
  | 'WINDOW_ERROR'
  | 'UNHANDLED_REJECTION'
  | 'ROUTER_ERROR'
  | 'API_SERVER_ERROR'

export interface FrontendExceptionReportPayload {
  errorCode: FrontendErrorCode
  requestId?: string
  httpStatus?: number
  httpMethod?: string
  requestPath?: string
  exceptionClass?: string
  summary: string
  stackTrace?: string
  frontendRoute?: string
  browserInfo?: string
  reportType?: 'FRONTEND' | 'API'
}

export interface SysExceptionLog {
  id: number
  source: 'BACKEND' | 'FRONTEND'
  level: string
  occurredAt: string
  errorCode: string | null
  requestId: string | null
  httpStatus: number | null
  httpMethod: string | null
  requestPath: string | null
  exceptionClass: string | null
  summary: string
  stackTrace: string | null
  frontendRoute: string | null
  browserInfo: string | null
  operatorId: number | null
  operatorUsername: string | null
  createdAt: string
}

export interface SysExceptionLogQuery {
  source?: 'BACKEND' | 'FRONTEND'
  httpStatus?: number
  exceptionClass?: string
  requestId?: string
  requestPath?: string
  keyword?: string
  occurredAtStart?: string
  occurredAtEnd?: string
  page?: number
  pageSize?: number
}

export interface ExceptionLogCleanupPayload {
  before: string
}
