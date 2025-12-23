{{/*
Expand the name of the chart.
*/}}
{{- define "ajasta-app.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "ajasta-app.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "ajasta-app.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "ajasta-app.labels" -}}
helm.sh/chart: {{ include "ajasta-app.chart" . }}
{{ include "ajasta-app.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "ajasta-app.selectorLabels" -}}
app.kubernetes.io/name: {{ include "ajasta-app.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
PostgreSQL labels
*/}}
{{- define "ajasta-app.postgres.labels" -}}
{{ include "ajasta-app.labels" . }}
app: ajasta
component: database
{{- end }}

{{/*
PostgreSQL selector labels
*/}}
{{- define "ajasta-app.postgres.selectorLabels" -}}
{{ include "ajasta-app.selectorLabels" . }}
app: ajasta
component: database
{{- end }}

{{/*
Backend labels
*/}}
{{- define "ajasta-app.backend.labels" -}}
{{ include "ajasta-app.labels" . }}
app: ajasta
component: backend
{{- end }}

{{/*
Backend selector labels
*/}}
{{- define "ajasta-app.backend.selectorLabels" -}}
{{ include "ajasta-app.selectorLabels" . }}
app: ajasta
component: backend
{{- end }}

{{/*
Frontend labels
*/}}
{{- define "ajasta-app.frontend.labels" -}}
{{ include "ajasta-app.labels" . }}
app: ajasta
component: frontend
{{- end }}

{{/*
Frontend selector labels
*/}}
{{- define "ajasta-app.frontend.selectorLabels" -}}
{{ include "ajasta-app.selectorLabels" . }}
app: ajasta
component: frontend
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "ajasta-app.serviceAccountName" -}}
{{- if .Values.rbac.serviceAccount.create }}
{{- default (include "ajasta-app.fullname" .) .Values.rbac.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.rbac.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
PostgreSQL service name
*/}}
{{- define "ajasta-app.postgres.serviceName" -}}
{{- .Values.postgres.service.name | default "ajasta-postgres" }}
{{- end }}

{{/*
Backend service name
*/}}
{{- define "ajasta-app.backend.serviceName" -}}
{{- .Values.backend.service.name | default "ajasta-backend" }}
{{- end }}

{{/*
Frontend service name
*/}}
{{- define "ajasta-app.frontend.serviceName" -}}
{{- .Values.frontend.service.name | default "ajasta-frontend" }}
{{- end }}

{{/*
Namespace
*/}}
{{- define "ajasta-app.namespace" -}}
{{- .Values.global.namespace | default .Values.namespace.name | default "ajasta" }}
{{- end }}
