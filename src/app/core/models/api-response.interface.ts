export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: ErrorDetails;
  timestamp: string;
  path?: string;
  trace_id?: string;
}

export interface ErrorDetails {
  code: string;
  message: string;
  details?: string;
  validation_errors?: { [key: string]: string };
  module?: string;
}