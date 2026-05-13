{{/*
Expand the name of the chart.
*/}}
{{- define "jhipster-online.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}
