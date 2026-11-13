# Dr. Annie

Dr. Annie is shown in the patient-facing app as the planned AI image-analysis assistant. It is not connected to a validated dental radiograph provider yet.

The image-analysis POST endpoints under `/api/ia/diagnostico-imagen` return HTTP 503 and do not create diagnoses, findings, or treatment recommendations while the provider is unavailable. The current modality-based mock output is intentionally disabled. Do not upload patient radiographs until a real provider and its privacy and clinical validation requirements are configured.

The patient role is limited in the frontend to the Dr. Annie status screen. Professional access and other roles continue to use the existing dashboard.
