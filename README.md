<div align="center">

# DriveON – Face Analysis (Android)

Análise em tempo real de fadiga do motorista usando CameraX + MediaPipe + ONNX, com alertas sonoros e histórico no Firebase.

![Android](https://img.shields.io/badge/Android-8.0%2B-brightgreen?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin)
![Gradle](https://img.shields.io/badge/Gradle-8.13-02303A?logo=gradle)
![AGP](https://img.shields.io/badge/AGP-8.12.3-3DDC84?logo=android)

</div>

## Visão Geral
O DriveON detecta sinais de fadiga a partir de landmarks faciais (MediaPipe) e classificação temporal (modelo ONNX). Ele:
- Mostra overlay de pontos faciais em tempo real (CameraX)
- Emite alertas (Microsleep e Bocejo) e exibe aviso de “Sinais de sono”
- Registra eventos no Firestore e apresenta relatórios diários
- Mantém sessão de login persistida e funciona offline após o primeiro login

## Recursos
- CameraX (frontal) com processamento contínuo
- MediaPipe Face Landmarker (landmarks 3D/precisos)
- ONNX Runtime para classificação temporal (Microsleep/Bocejo/Atento)
- Alertas sonoros (loops/bipes) via `AlertManager`
- Relatórios (Pizza/Barras/Radar) com legendas que não se sobrepõem (word‑wrap)
- Firestore com cache/persistência offline habilitado

## Arquitetura (alto nível)
```
CameraX -> Frames -> MediaPipe FaceLandmarker -> Landmarks
       -> Extração de features (normalização + pontos) -> Buffer temporal (SEQ_LEN)
       -> ONNX Runtime (LSTM) -> Decisão (microsleep/bocejo/atento)
       -> UI (overlay + status + contadores) -> Alertas -> Firestore (events)
```

## Avaliação e Métricas

### Parâmetros alternativos

| Nível de sensibilidade | Limiar Microsleep (ms olhos fechados) | Bocejos consecutivos | Aplicação sugerida |
| --- | --- | --- | --- |
| **Alta** | 1.200 ms | 2 | Turnos noturnos/frotas com histórico de fadiga. Maior chance de falsos positivos. |
| **Média (default)** | 2.000 ms | 3 | Situações gerais, equilíbrio entre alerta e conforto do motorista. |
| **Baixa** | 2.800 ms | 4 | Testes iniciais ou motoristas que reclamam de excesso de avisos. |

Outro parâmetro relevante:
- EAR/MAR adaptativos via histórico (`HISTORY_SIZE = 5`) reduzem oscilações do LSTM em faces parcialmente visíveis.

### Latência no dispositivo
- A tela de análise exibe a latência média do pipeline (MediaPipe + featurização + decisão) usando uma média exponencial (20% das amostras mais recentes).  
- O valor mostrado também inclui o FPS efetivo (`fps = 1000/latência`), possibilitando comparar diferentes aparelhos antes de implantar em campo.  
## Estrutura do Projeto
```
app/
  src/main/java/com/example/faceanalysis/
    AnalysisActivity.kt · AlertManager.kt · OverlayView.kt
    LoginActivity.kt · RegisterActivity.kt · HomeActivity.kt · MainActivity.kt
    ReportActivity.kt · ProfileActivity.kt · PrivacyActivity.kt
  src/main/res/ (layouts, values, raw)
  src/main/assets/ (face_landmarker.task, model_lstm_*.onnx)
  AndroidManifest.xml
```

## Instalação
1) Pré‑requisitos
- Android Studio (Giraffe+), JDK 17
- Dispositivo real com câmera frontal (recomendado)

2) Firebase
- Adicione seu arquivo `app/google-services.json` (não é versionado neste repositório público)
- Habilite Auth por E‑mail/Senha no Firebase Console

3) Build & Run
- Sincronize o Gradle e execute o módulo `app` em um dispositivo real

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

## Notas Importantes
- Sessão persistida (auto‑login) e Firestore offline habilitado
- Overlay espelhado para câmera frontal e PreviewView `fillCenter` para melhor alinhamento
- O arquivo `app/google-services.json` não deve ser versionado (já ignorado no `.gitignore`)

## Troubleshooting
- PERMISSION_DENIED no relatório: confira as regras acima e se o `uid` do documento em `users/{uid}` é o mesmo do usuário autenticado
- “No chart data available” com eventos: verifique se o dia possui eventos ativos (Microsleep/Bocejo/Atento/Sem Rosto)
- Build com pouca memória (Windows):
  ```
  ./gradlew.bat --stop
  ./gradlew.bat clean :app:assembleDebug --no-daemon
  ```

## Licença
Defina a licença do projeto (MIT/Apache 2.0, etc.) conforme sua necessidade.
