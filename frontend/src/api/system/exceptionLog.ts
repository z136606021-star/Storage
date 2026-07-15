import { http } from '@/api/http'
import type { PageResult } from '@/types/common'
import type {
  ExceptionLogCleanupPayload,
  FrontendExceptionReportPayload,
  SysExceptionLog,
  SysExceptionLogQuery,
} from '@/types/system/exceptionLog'

export async function fetchExceptionLogPage(
  query: SysExceptionLogQuery,
): Promise<PageResult<SysExceptionLog>> {
  const { data } = await http.get<PageResult<SysExceptionLog>>('/system/exception-logs', {
    params: query,
  })
  return data
}

export async function fetchExceptionLogDetail(id: number): Promise<SysExceptionLog> {
  const { data } = await http.get<SysExceptionLog>(`/system/exception-logs/${id}`)
  return data
}

export async function reportFrontendException(
  payload: FrontendExceptionReportPayload,
): Promise<void> {
  await http.post('/system/exception-logs/report', payload)
}

export async function cleanupExceptionLogs(
  payload: ExceptionLogCleanupPayload,
): Promise<{ deleted: number }> {
  const { data } = await http.delete<{ deleted: number }>('/system/exception-logs/cleanup', {
    data: payload,
  })
  return data
}
