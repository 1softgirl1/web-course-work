export type UserRole = 'PATIENT' | 'DOCTOR' | 'DOCTOR_EXTENDED'

export interface LoginRequest {
  login: string
  password: string
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
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
  tokenType: 'Bearer'
  expiresAt: string
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

export interface ListDoctorPatientsParams {
  scope?: 'own' | 'all'
  page?: number
  limit?: number
  regionId?: number
  diagnosis?: string
}
