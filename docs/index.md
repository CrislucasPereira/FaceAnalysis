---
title: DriveON – Face Analysis (Android)
description: Detecção de fadiga em tempo real no Android (CameraX + MediaPipe + ONNX + Firebase)
---

# Visão Geral

DriveON é um aplicativo Android para detecção de fadiga do motorista a partir de landmarks faciais (MediaPipe) e classificação temporal (modelo ONNX). O app emite alertas, registra eventos no Firebase e apresenta relatórios diários (MPAndroidChart).

- Plataforma: Android (Kotlin, AGP 8, Gradle 8)
- Visão: reduzir risco por fadiga/sonolência em condução
- Status: v10 Final

## Recursos
- CameraX (frontal) com processamento contínuo e overlay de landmarks
- MediaPipe Face Landmarker (preciso, rápido)
- ONNX Runtime (LSTM) para estados: Microsleep, Bocejo, Atento, Sem Rosto, Sinais de Sono
- Alertas sonoros e diálogo de aviso
- Relatórios (pizza/barras/radar) com legendas que não se sobrepõem
- Firestore com cache/persistência offline habilitado
- Sessão persistida (auto‑login)

## Arquitetura (alto nível)
```
CameraX -> MediaPipe FaceLandmarker -> Landmarks
       -> Extração de features (normalização + pontos) -> Buffer temporal (SEQ_LEN)
       -> ONNX Runtime (LSTM) -> Decisão (microsleep/bocejo/atento)
       -> UI (overlay + status + contadores) -> Alertas -> Firestore (events)
```

## Estrutura de Pastas
```
app/
  src/main/java/com/example/faceanalysis/
    AnalysisActivity.kt · AlertManager.kt · OverlayView.kt
    LoginActivity.kt · RegisterActivity.kt · HomeActivity.kt · MainActivity.kt
    ReportActivity.kt · ProfileActivity.kt · PrivacyActivity.kt
  src/main/res/ (layouts, values, raw)
  src/main/assets/ (face_landmarker.task, model_lstm_*.onnx)
```

## Instalação Rápida
1. Coloque seu `app/google-services.json` (não é versionado neste repositório público)
2. Habilite Auth por E‑mail/Senha no Firebase Console
3. Sincronize o Gradle e execute o módulo `app` em um dispositivo real

## Firestore
Estrutura dos eventos
```
users/{uid}/events (document)
  status: "Microsleep" | "Bocejo" | "Atento" | "Sem Rosto" | "Sinais de Sono"
  startTime, endTime, duration (ms)
  ear, mar, confidence, device
  startReadable, endReadable
```

Regras sugeridas
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      match /events/{eventId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }
  }
}
```

## Troubleshooting
- PERMISSION_DENIED no relatório: confira as regras acima e se o `uid` do documento em `users/{uid}` é o mesmo do usuário autenticado
- “No chart data available” com eventos: verifique se o dia possui eventos ativos (Microsleep/Bocejo/Atento/Sem Rosto)
- Build com pouca memória (Windows):
  ```
  ./gradlew.bat --stop
  ./gradlew.bat clean :app:assembleDebug --no-daemon
  ```

## Roadmap
- Migrar YUV→RGB para implementação sem RenderScript
- I18n completa e Parcelable no histórico
- Testes instrumentados mínimos do pipeline

## Changelog
- v10 Final
  - Legendas dos gráficos com word wrap; espaçamento entre cards
  - Auto‑login e Firestore offline
  - Overlay espelhado e PreviewView `fillCenter`
  - Correções de acentuação e limpeza de comentários

