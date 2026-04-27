export type UserRole = 'PATIENT' | 'DOCTOR' | 'DOCTOR_EXTENDED'
export type UserStatus = 'ACTIVE' | 'INACTIVE'

export interface LoginRequest {
  username: string
  password: string
}

export interface RefreshTokenRequest {
  refreshToken: string
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
  refreshToken: string
}

export interface AuthUserResponse {
  id: number
  role: UserRole
  displayName: string
  email: string | null
  patientCode: string | null
}

export interface AuthResponse {
  accessToken: string
  tokenType: string
  accessTokenExpiresAt: string
  refreshToken: string
  refreshTokenExpiresAt: string
  user: AuthUserResponse
}

export interface ValveResponse {
  name: string
  size: string
  material: string
}

export interface OperationParametersResponse {
  anesthesia: string
  durationMinutes: number
  deliverySystem: string
}

export interface CreateOperationParametersRequest extends OperationParametersResponse {}

export interface CreateValveRequest extends ValveResponse {}

export interface CreatePatientRequest {
  lastName: string
  firstName: string
  middleName: string | null
  birthDate: string
  diagnosis: string
  regionId: number
  valve: CreateValveRequest
  operationParameters: CreateOperationParametersRequest
  medications: string
}

export interface CreateDoctorRequest {
  username: string
  role: UserRole
  lastName: string
  firstName: string
  middleName: string | null
  specialization: string
  workplace: string
  regionId: number
}

export interface DoctorResponse {
  id: number
  userId: number
  username: string
  role: UserRole
  status: UserStatus
  lastName: string
  firstName: string
  middleName: string | null
  specialization: string
  workplace: string
  regionId: number
  regionName: string
}

export interface CreatedDoctorResponse {
  doctor: DoctorResponse
  temporaryPassword: string
}

export interface DoctorListResponse {
  items: DoctorResponse[]
  page: number
  limit: number
  total: number
}

export interface UpdateDoctorRequest {
  username?: string | null
  role?: UserRole | null
  status?: UserStatus | null
  lastName?: string | null
  firstName?: string | null
  middleName?: string | null
  specialization?: string | null
  workplace?: string | null
  regionId?: number | null
}

export interface DoctorPasswordResetResponse {
  id: number
  username: string
  temporaryPassword: string
}

export interface UpdatePatientRequest {
  lastName?: string
  firstName?: string
  middleName?: string | null
  birthDate?: string
  diagnosis?: string
  regionId?: number
  medications?: string
  valve?: CreateValveRequest
  operationParameters?: CreateOperationParametersRequest
  password?: string
}

export type PatientStatus = 'GREEN' | 'YELLOW' | 'RED'
export type PatientViewMode = 'FULL' | 'ANONYMIZED'

export interface CreatedPatientResponse {
  id: number
  patientCode: string
  temporaryPassword: string
  lastName: string
  firstName: string
  middleName: string | null
  birthDate: string
  diagnosis: string
  regionId: number
  valve: ValveResponse
  operationParameters: OperationParametersResponse
  medications: string
  createdAt: string
}

export interface PatientSummaryResponse {
  id: number
  patientCode: string
  lastName: string | null
  firstName: string | null
  middleName: string | null
  birthDate: string
  diagnosis: string
  regionId: number
  status: PatientStatus
  valve: ValveResponse
  operationParameters: OperationParametersResponse
  medications: string
  createdAt: string
  lastExaminationAt: string | null
}

export interface PatientListResponse {
  items: PatientSummaryResponse[]
  page: number
  limit: number
  total: number
}

export interface MeasurementResponse {
  characteristicId: number
  characteristicCode: string
  characteristicName: string
  value: string
  unit: string
  comment: string | null
}

export interface ExaminationResponse {
  examId: number
  title: string
  examDate: string
  comment: string | null
  measurements: MeasurementResponse[]
}

export interface ExaminationListResponse {
  items: ExaminationResponse[]
}

export interface PatientCardResponse {
  id: number
  viewMode: PatientViewMode
  patientCode: string
  lastName: string | null
  firstName: string | null
  middleName: string | null
  regionId: number
  regionName: string
  birthDate: string
  diagnosis: string
  valve: ValveResponse
  operationParameters: OperationParametersResponse
  medications: string
  createdAt: string
  vitalsHistory: ExaminationResponse[]
}

export interface CreateExaminationMeasurementRequest {
  characteristicCode: string
  value: number
  comment: string | null
}

export interface CreateExaminationRequest {
  title: string
  examDate: string
  comment: string | null
  measurements: CreateExaminationMeasurementRequest[]
}

export interface UpdateExaminationMeasurementRequest {
  characteristicCode: string
  value?: number | null
  comment?: string | null
}

export interface UpdateExaminationRequest {
  title?: string | null
  examDate?: string | null
  comment?: string | null
  measurements?: UpdateExaminationMeasurementRequest[] | null
}

export interface ListDoctorPatientsParams {
  scope?: 'own' | 'all'
  page?: number
  limit?: number
  regionId?: number
  diagnosis?: string
}

export interface ListDoctorsParams {
  page?: number
  limit?: number
  regionId?: number
  role?: UserRole
  status?: UserStatus
  search?: string
}

export interface ApiErrorResponse {
  status: number
  error: string
  message: string
  details: string[]
  timestamp: string
}
