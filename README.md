<div align="center">

# DriveON - Face Analysis (Android)

Analise em tempo real de fadiga do motorista usando CameraX + MediaPipe + ONNX, com alertas sonoros e historico no Firebase.

![Android](https://img.shields.io/badge/Android-8.0%2B-brightgreen?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin)
![Gradle](https://img.shields.io/badge/Gradle-8.13-02303A?logo=gradle)
![AGP](https://img.shields.io/badge/AGP-8.12.3-3DDC84?logo=android)

</div>

## Visao Geral
O DriveON detecta sinais de fadiga a partir de landmarks faciais (MediaPipe) e classificacao temporal (modelo ONNX). Ele:
- Mostra overlay de pontos faciais em tempo real (CameraX)
- Emite alertas (Microsleep e Bocejo) e exibe aviso de "Sinais de sono"
- Registra eventos no Firestore e apresenta relatorios diarios
- Mantem sessao de login persistida e funciona offline apos o primeiro login

## Recursos
- CameraX (frontal) com processamento continuo
- MediaPipe Face Landmarker (landmarks 3D/precisos)
- ONNX Runtime para classificacao temporal (Microsleep/Bocejo/Atento)
- Alertas sonoros (loops/bipes) via `AlertManager`
- Relatorios (Pizza/Barras/Radar) com legendas que nao se sobrepoem (word-wrap)
- Firestore com cache/persistencia offline habilitado

## Arquitetura (alto nivel)
```
CameraX -> Frames -> MediaPipe FaceLandmarker -> Landmarks
       -> Extracao de features (normalizacao + pontos) -> Buffer temporal (SEQ_LEN)
       -> ONNX Runtime (LSTM) -> Decisao (microsleep/bocejo/atento)
       -> UI (overlay + status + contadores) -> Alertas -> Firestore (events)
```

## Avaliacao e metricas

### Parametros alternativos

| Nivel de sensibilidade | Limiar Microsleep (ms olhos fechados) | Bocejos consecutivos | Aplicacao sugerida |
| --- | --- | --- | --- |
| **Alta** | 1.200 ms | 2 | Turnos noturnos/frotas com historico de fadiga. Maior chance de falsos positivos. |
| **Media (default)** | 2.000 ms | 3 | Situacoes gerais, equilibrio entre alerta e conforto do motorista. |
| **Baixa** | 2.800 ms | 4 | Testes iniciais ou motoristas que reclamam de excesso de avisos. |

Outro parametro relevante:
- EAR/MAR adaptativos via historico (`HISTORY_SIZE = 5`) reduzem oscilacoes do LSTM em faces parcialmente visiveis.

### Latencia no dispositivo
- A tela de analise exibe a latencia media do pipeline (MediaPipe + feiturizacao + decisao) usando uma media exponencial (20% das amostras mais recentes).  
- O valor mostrado tambem inclui o FPS efetivo (`fps = 1000/latencia`), possibilitando comparar diferentes aparelhos antes de implantar em campo.  

## Estrutura do Projeto
```
app/
  src/main/java/com/example/faceanalysis/
    AnalysisActivity.kt - AlertManager.kt - OverlayView.kt
    LoginActivity.kt - RegisterActivity.kt - HomeActivity.kt - MainActivity.kt
    ReportActivity.kt - ProfileActivity.kt - PrivacyActivity.kt
  src/main/res/ (layouts, values, raw)
  src/main/assets/ (face_landmarker.task, model_lstm_*.onnx)
  AndroidManifest.xml
```

## Instalacao
1) Pre-requisitos
- Android Studio (Giraffe+), JDK 17
- Dispositivo real com camera frontal (recomendado)

2) Firebase
- Adicione seu arquivo `app/google-services.json` (nao e versionado neste repositorio publico)
- Habilite Auth por email/senha no Firebase Console

3) Build & Run
- Sincronize o Gradle e execute o modulo `app` em um dispositivo real

## Firestore (estrutura e regras)
Estrutura de documentos dos eventos:

```
users/{uid}/events (document)
  status: "Microsleep" | "Bocejo" | "Atento" | "Sem Rosto" | "Sinais de Sono"
  startTime, endTime, duration (ms)
  ear, mar, confidence, device
  startReadable, endReadable
```

Regras sugeridas (copie e publique no editor de regras do Firestore):

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

## Notas importantes
- Sessao persistida (autologin) e Firestore offline habilitado
- Overlay espelhado para camera frontal e PreviewView `fillCenter` para melhor alinhamento
- O arquivo `app/google-services.json` nao deve ser versionado (ja ignorado no `.gitignore`)

## Troubleshooting
- PERMISSION_DENIED no relatorio: confira as regras acima e se o `uid` do documento em `users/{uid}` e o mesmo do usuario autenticado
- "No chart data available" com eventos: verifique se o dia possui eventos ativos (Microsleep/Bocejo/Atento/Sem Rosto)
- Build com pouca memoria (Windows):
  ```
  ./gradlew.bat --stop
  ./gradlew.bat clean :app:assembleDebug --no-daemon
  ```

## Licenca
Defina a licenca do projeto (MIT/Apache 2.0, etc.) conforme sua necessidade.
