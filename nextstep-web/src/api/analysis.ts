import { request } from './http'

export interface DimensionScore { name: string; score: number }
export interface Recommendation {
  type: string
  refId: number
  title: string
  subtitle: string
  matchScore: number
  tag: string
}
export interface PathScore {
  path: string
  pathName: string
  overall: number
  dimensions: DimensionScore[]
  advice: string[]
  recommendations: Recommendation[]
}
export interface AnalysisResult {
  userId: number
  analyzedAt: string
  profileCompleteness: number
  paths: PathScore[]
  topPath?: string
  topPathReason?: string
}

export const analysisApi = {
  score: () => request<AnalysisResult>({ url: '/analysis/score' })
}

export const aiApi = {
  explainCache: () => request<string | null>({ url: '/ai/explain/cache' })
}
