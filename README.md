# DriveON -- Face Analysis (Android)

Análise em tempo real de fadiga do motorista usando CameraX +
MediaPipe + ONNX, com alertas sonoros e histórico no Firebase.

![Android](https://img.shields.io/badge/Android-8.0%2B-brightgreen?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin)
![Gradle](https://img.shields.io/badge/Gradle-8.13-02303A?logo=gradle)
![AGP](https://img.shields.io/badge/AGP-8.12.3-3DDC84?logo=android)
:::

## Visão Geral

O DriveON detecta sinais de fadiga a partir de **landmarks faciais
(MediaPipe)** e **classificação temporal (modelo ONNX)**. Ele:

-   Mostra overlay de pontos faciais em tempo real (CameraX)
-   Emite alertas (Microsleep e Bocejo) e exibe aviso de "Sinais de
    Sono"
-   Registra eventos no Firestore e gera relatórios diários
-   Mantém sessão de login persistida e funciona offline após o primeiro
    login

## Recursos

-   CameraX (frontal) com processamento contínuo\
-   MediaPipe Face Landmarker (landmarks 3D de alta precisão)\
-   ONNX Runtime (modelo LSTM) para detecção temporal\
-   Alertas sonoros (loops e bipes) via `AlertManager`\
-   Relatórios (Pizza, Barras e Radar) com legendas ajustáveis
    (word-wrap)\
-   Firestore com cache e persistência offline

## Arquitetura (alto nível)

    CameraX -> Frames -> MediaPipe FaceLandmarker -> Landmarks
           -> Extração de features (normalização + pontos) -> Buffer temporal (SEQ_LEN)
           -> ONNX Runtime (LSTM) -> Decisão (microsleep / bocejo / atento)
           -> UI (overlay + status + contadores) -> Alertas -> Firestore (events)

## Avaliação e Métricas

### Parâmetros de Sensibilidade

  ----------------------------------------------------------------------------
  Sensibilidade   Limiar Microsleep     Bocejos consecutivos Aplicação
                  (ms)                                       sugerida
  --------------- --------------------- -------------------- -----------------
  **Alta**        1200 ms               2                    Turnos
                                                             noturnos/frotas
                                                             com histórico de
                                                             fadiga. Mais
                                                             falsos positivos.

  **Média**       2000 ms               3                    Uso geral.
  (default)                                                  Equilíbrio entre
                                                             alerta e
                                                             conforto.

  *Baixa*       2800 ms               4                    Testes iniciais
                                                             ou motoristas que
                                                             reclamam de
                                                             excesso de
                                                             avisos.
  ----------------------------------------------------------------------------

### Latência no dispositivo

-   Latência média do pipeline + FPS exibidos na tela de análise\
-   Média exponencial baseada nos últimos 20% dos frames

## Estrutura do Projeto

    app/
      src/main/java/com/example/faceanalysis/
        AnalysisActivity.kt
        AlertManager.kt
        OverlayView.kt
        LoginActivity.kt
        RegisterActivity.kt
        HomeActivity.kt
        MainActivity.kt
        ReportActivity.kt
        ProfileActivity.kt
        PrivacyActivity.kt
      src/main/res/ (layouts, values, raw)
      src/main/assets/ (face_landmarker.task, model_lstm_*.onnx)
      AndroidManifest.xml

## Instalação

### 1) Pré-requisitos

-   Android Studio Giraffe+\
-   JDK 17\
-   Dispositivo real com câmera frontal (recomendado)

### 2) Firebase

-   Adicione seu arquivo **`app/google-services.json`**
-   Habilite autenticação por **E-mail/Senha** no Firebase Console

### 3) Build & Run

-   Sincronize o Gradle\
-   Rode o módulo `app` em um dispositivo físico

## Firestore (estrutura e regras)

### Estrutura de documentos

    users/{uid}/events/{eventId}
      status: "Microsleep" | "Bocejo" | "Atento" | "Sem Rosto" | "Sinais de Sono"
      startTime
      endTime
      duration (ms)
      ear
      mar
      confidence
      device
      startReadable
      endReadable

### Regras sugeridas

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

## Notas Importantes

-   Sessão persistida (auto-login)\
-   Firestore offline habilitado\
-   Overlay espelhado\
-   PreviewView com `fillCenter`\
-   `google-services.json` não deve ser versionado

## Troubleshooting

### PERMISSION_DENIED no relatório

-   Verifique as regras do Firestore\
-   Confirme se o `uid` coincide com o usuário autenticado

### "No chart data available"

-   Verifique se existem eventos registrados naquele dia

### Erros de memória

    ./gradlew.bat --stop
    ./gradlew.bat clean :app:assembleDebug --no-daemon

## Licença

Defina a licença conforme a necessidade (MIT, Apache 2.0 etc.).
