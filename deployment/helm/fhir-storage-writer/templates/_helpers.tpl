{{/*
Expand the name of the chart.
*/}}
{{- define "fhir-storage-writer.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "fhir-storage-writer.fullname" -}}
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

{{- define "fhir-storage-writer.fullversionname" -}}
{{- if .Values.istio.enable }}
{{- $name := include "fhir-storage-writer.fullname" . }}
{{- $version := regexReplaceAll "\\.+" .Chart.Version "-" }}
{{- printf "%s-%s" $name $version | trunc 63 }}
{{- else }}
{{- include "fhir-storage-writer.fullname" . }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "fhir-storage-writer.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "fhir-storage-writer.labels" -}}
helm.sh/chart: {{ include "fhir-storage-writer.chart" . }}
{{ include "fhir-storage-writer.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- if .Values.istio.enable }}
version: {{ .Chart.AppVersion | quote }}
{{- end }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- with .Values.customLabels }}
{{ toYaml . }}
{{- end }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "fhir-storage-writer.selectorLabels" -}}
{{- if .Values.istio.enable }}
app: {{ include "fhir-storage-writer.name" . }}
version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/name: {{ include "fhir-storage-writer.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Deployment labels
*/}}
{{- define "fhir-storage-writer.deploymentLabels" -}}
{{ if .Values.istio.enable -}}
istio-validate-jwt: "{{ .Values.istio.validateJwt | required ".Values.istio.validateJwt is required" }}"
{{- with .Values.deploymentLabels }}
{{ toYaml . }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "fhir-storage-writer.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "fhir-storage-writer.fullversionname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Environment Variables
*/}}
{{- define "fhir-storage-writer.env" -}}
{{- $envs := dict -}}
{{- if .Values.secret -}}
{{- $envs = set $envs "SPRING_CONFIG_IMPORT" "configtree:/secrets/*/" }}
{{- end -}}
{{- if .Values.customEnvVars -}}
{{- range $key, $value := .Values.customEnvVars -}}
{{ if $value -}}
{{- $envs = set $envs $key $value }}
{{- end -}}
{{- end -}}
{{- end -}}
{{- if .Values.debug.enable -}}
{{- $toolOptions := printf "%s %s" (get $envs "JAVA_TOOL_OPTIONS") .Values.debug.params | trim -}}
{{- $envs = set $envs "JAVA_TOOL_OPTIONS" $toolOptions -}}
{{- end -}}
{{- range $i, $key := keys $envs | sortAlpha -}}
{{- if $i }}
{{ end -}}
{{- $v := get $envs $key -}}
- name: {{ $key | quote }}
{{- if kindIs "string" $v }}
  value: {{ tpl $v . | quote }}
{{- else }}
  value: {{ $v | quote }}
{{- end }}
{{- end -}}
{{- end -}}
