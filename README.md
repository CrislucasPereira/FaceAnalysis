<div align="center">

# DriveON - Face Analysis (Android)

Análise em tempo real de fadiga do motorista usando CameraX + MediaPipe + ONNX, com alertas sonoros e histórico no Firebase.

![Android](https://img.shields.io/badge/Android-8.0%2B-brightgreen?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin)
![Gradle](https://img.shields.io/badge/Gradle-8.13-02303A?logo=gradle)
![AGP](https://img.shields.io/badge/AGP-8.12.3-3DDC84?logo=android)
![License](https://img.shields.io/badge/License-TBD-lightgrey)

</div>

## Sumário
- Visão Geral
- Demonstração
- Recursos
- Arquitetura
- Stack
- Estrutura do Projeto
- Instalação e Execução
- Configuração do Firebase
- Regras do Firestore
- Permissões
- Modelos e Assets
- Fluxos de Uso
- Troubleshooting
- Notas da Versão Final
- Roadmap
- Contribuindo
- Licença
- Agradecimentos

## Visão Geral
O DriveON é um app Android que detecta sinais de fadiga do motorista a partir de landmarks faciais (MediaPipe) e classificação temporal (modelo ONNX). O app:
- Mostra overlay dos pontos faciais em tempo real (CameraX).
- Emite alertas sonoros (microsleep e bocejo) e diálogo de atenção.
- Registra eventos no Firestore (com métricas) e apresenta relatórios diários.
- Mantém sessão de login persistida e funciona offline após o primeiro login (cache do Firestore).

## Demonstração
- Coloque screenshots em `docs/screenshots/` (analysis.png, history.png, report.png).

## Recursos
- CameraX (frontal) com processamento contínuo.
- MediaPipe Face Landmarker para landmarks faciais.
- Classificação temporal com ONNX Runtime:
  - Microsleep (olhos fechados sustentados)
  - Bocejo (abertura de boca sustentada)
- Alertas sonoros por tipo de evento (loops/bipes) e aviso de “Sinais de sono”.
- Histórico + persistência no Firestore (com cache offline).
- Relatórios com gráficos (Pizza/Barras/Radar) por dia e legendas com word wrap.

## Arquitetura
```
CameraX -> Frames -> MediaPipe FaceLandmarker -> Landmarks
       -> Extração de features (normalização + pontos selecionados)
       -> Buffer temporal (SEQ_LEN)
       -> ONNX Runtime (modelo LSTM)
       -> Decisão de estado (microsleep/bocejo/atento)
       -> UI (overlay + status + contadores) + Alertas sonoros
       -> Persistência (Firestore users/{uid}/events)
```

Principais classes:
- AnalysisActivity.kt — pipeline de análise, estados, persistência.
- AlertManager.kt — orquestra sons/loops.
- OverlayView.kt — overlay dos landmarks.
- ReportActivity.kt — gráficos/relatórios (MPAndroidChart).
- Telas: LoginActivity, RegisterActivity, ForgotPassword, MainActivity, HomeActivity, ProfileActivity, PrivacyActivity.

## Stack
- Android (Kotlin, Gradle 8.13, AGP 8.12.3, JDK 17)
- CameraX
- MediaPipe Tasks — `face_landmarker.task`
- ONNX Runtime — `model_lstm_3_45_euclidean.onnx`
- Firebase — Auth + Firestore
- MPAndroidChart

## Estrutura do Projeto
```
app/
└─ src/main/java/com/example/faceanalysis/
   ├─ AnalysisActivity.kt · AlertManager.kt · OverlayView.kt
   ├─ LoginActivity.kt · RegisterActivity.kt · ForgotPassword.kt
   ├─ MainActivity.kt · HomeActivity.kt · ProfileActivity.kt · PrivacyActivity.kt
   └─ ReportActivity.kt
└─ src/main/res/ (layouts, values, raw)
└─ src/main/assets/ (modelos MediaPipe/ONNX)
└─ AndroidManifest.xml
```

## Instalação e Execução
Pré-requisitos: Android Studio (Giraffe+), JDK 17, dispositivo com câmera frontal.

Passos:
1. Abra o projeto no Android Studio.
2. Garanta `app/google-services.json` válido.
3. Sincronize o Gradle.
4. Execute o módulo `app` em um dispositivo real.

CLI (Windows):
```
./gradlew.bat clean :app:assembleDebug --no-daemon
```

## Configuração do Firebase
Auth: E-mail/Senha habilitado.

Estrutura no Firestore:
```
users/{uid}/events (document)
  status: "Microsleep" | "Bocejo" | "Atento" | "Sem Rosto" | "Sinais de Sono"
  startTime, endTime, duration (ms)
  ear, mar, confidence, device
  startReadable, endReadable (strings)
```

## Regras do Firestore
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

## Permissões
- `android.permission.INTERNET`
- `android.permission.CAMERA`

## Modelos e Assets
- `face_landmarker.task` — landmarks faciais (MediaPipe Tasks)
- `model_lstm_3_45_euclidean.onnx` — classificador temporal
- Sons de alerta em `res/raw/`

## Fluxos de Uso
1. Login/Registro (sessão persistida; auto-login na próxima abertura).
2. Iniciar análise (CameraX + MediaPipe) e acompanhar status/contadores.
3. Alertas ao atingir limiares (microsleep/bocejos).
4. Relatórios diários (funcionam offline via cache).

## Troubleshooting
- Firestore PERMISSION_DENIED no relatório: confira as regras e o UID do usuário.
- “No chart data available” com eventos: confirme que o dia possui eventos ativos (Desatenção foi removida).
- Sem memória no build (Windows):
  ```
  ./gradlew.bat --stop
  ./gradlew.bat clean :app:assembleDebug --no-daemon
  ```

## Notas da Versão Final
- Removida a detecção/UI de “Desatenção”.
- Textos e acentos corrigidos; mensagem “Sem dados disponíveis” no relatório.
- Legendas de gráficos com word wrap; espaçamento entre cards.
- Campo de senha com botão de visibilidade (padrão sempre oculto).
- Auto-login e Firestore offline habilitados.
- Landmarks alinhados: overlay espelhado para câmera frontal e preview `fillCenter`.

## Roadmap
- Migrar YUV->RGB sem RenderScript.
- I18n completa e Parcelable no histórico.
- Testes instrumentados mínimos do pipeline.

