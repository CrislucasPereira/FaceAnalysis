---
title: DriveON – Face Analysis (Android)
description: Detecção de fadiga em tempo real no Android (CameraX + MediaPipe + ONNX + Firebase)
---

<link rel="stylesheet" href="assets/css/custom.css" />

<section class="hero">
  <div class="banner"></div>
  <div class="title-stack">
    <div class="title-box">
      <h1><span class="brand-drive">Drive</span><span class="brand-on">On</span></h1>
    </div>
    <p class="subtitle">Características do Relatório Técnico</p>
  </div>
  <div class="badges">
    <img alt="Android" src="https://img.shields.io/badge/Android-8.0%2B-brightgreen?logo=android" />
    <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin" />
    <img alt="Gradle" src="https://img.shields.io/badge/Gradle-8.13-02303A?logo=gradle" />
    <img alt="AGP" src="https://img.shields.io/badge/AGP-8.12.3-3DDC84?logo=android" />
  </div>
  <div class="cta">
    <a class="btn primary-dark" href="https://github.com/CrislucasPereira/FaceAnalysis">Ver no GitHub</a>
    <a class="btn outline-yellow" href="#instalacao">Começar agora</a>
    <a class="btn gradient" href="https://github.com/CrislucasPereira/FaceAnalysis/releases/tag/v10">Download v10</a>
  </div>
</section>

## Visão Geral

DriveON detecta sinais de fadiga a partir de landmarks faciais (MediaPipe) e classificação temporal (ONNX). Emite alertas, registra eventos no Firebase e apresenta relatórios com MPAndroidChart.

- Plataforma: Android (Kotlin, AGP 8, Gradle 8)
- Visão: reduzir risco por fadiga/sonolência em condução
- Status: v10 Final

## Recursos

<div class="grid">
  <div class="card">
    <h3>Detecção em tempo real</h3>
    <p>CameraX + MediaPipe Face Landmarker com overlay de pontos faciais.</p>
  </div>
  <div class="card">
    <h3>Classificação temporal</h3>
    <p>Modelo ONNX (LSTM) para Microsleep, Bocejo, Atento e Sem Rosto.</p>
  </div>
  <div class="card">
    <h3>Relatórios claros</h3>
    <p>Gráficos Pizza/Barras/Radar com legendas que não se sobrepõem.</p>
  </div>
  <div class="card">
    <h3>Confiável offline</h3>
    <p>Firestore com cache/persistência offline e sessão persistida (auto‑login).</p>
  </div>
</div>

## Arquitetura (alto nível)
```
CameraX -> MediaPipe FaceLandmarker -> Landmarks
       -> Extração de features (normalização + pontos) -> Buffer temporal (SEQ_LEN)
       -> ONNX Runtime (LSTM) -> Decisão (microsleep/bocejo/atento)
       -> UI (overlay + status + contadores) -> Alertas -> Firestore (events)
```

## Avaliação e Métricas

### Parâmetros alternativos
| Sensibilidade | Microsleep (ms) | Bocejos necessários | Uso |
| --- | --- | --- | --- |
| Alta | 1.200 | 2 | Plantões longos / maior vigilância |
| Média (padrão) | 2.000 | 3 | Equilíbrio alerta × conforto |
| Baixa | 2.800 | 4 | Testes ou motoristas sensíveis a alarmes |

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
<a id="instalacao"></a>
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

<p class="credits">Crislucas Pereira – Ednei Romão – Guilherme Nishiyama – Victor Nunes</p>

## Changelog
- v10 Final
  - Legendas dos gráficos com word wrap; espaçamento entre cards
  - Auto‑login e Firestore offline
  - Overlay espelhado e PreviewView `fillCenter`
  - Correções de acentuação e limpeza de comentários
