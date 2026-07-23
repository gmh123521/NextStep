import { request } from './http'

export interface UserProfile {
  id?: number
  userId?: number
  currentSchool?: string
  schoolLevel?: string
  currentMajor?: string
  majorCategory?: string
  degreeType?: string
  gradeYear?: number
  gpa?: number
  gpaScale?: number
  classRankPct?: number
  englishLevel?: string
  englishScore?: number
  hasResearch?: number
  hasInternship?: number
  hasCompetition?: number
  hasPaper?: number
  targetPaths?: string
  preferredRegions?: string
  preferredIndustries?: string
  salaryExpectation?: number
  riskAppetite?: number
  monthlyBudget?: number
  interests?: string
  strengths?: string
  weaknesses?: string
  currentStatus?: string
  profileCompleteness?: number
}

export const profileApi = {
  get: () => request<UserProfile | null>({ url: '/user/profile' }),
  upsert: (data: UserProfile) => request<UserProfile>({ url: '/user/profile', method: 'PUT', data })
}
