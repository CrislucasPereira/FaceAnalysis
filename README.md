<div align="center">

# DriveON ÔÇô Face Analysis (Android)

An├ílise em tempo real de fadiga do motorista usando CameraX + MediaPipe + ONNX, com alertas sonoros e hist├│rico no Firebase.

![Android](https://img.shields.io/badge/Android-8.0%2B-brightgreen?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin)
![Gradle](https://img.shields.io/badge/Gradle-8.13-02303A?logo=gradle)
![AGP](https://img.shields.io/badge/AGP-8.12.3-3DDC84?logo=android)

</div>

## Vis├úo Geral
O DriveON detecta sinais de fadiga a partir de landmarks faciais (MediaPipe) e classifica├º├úo temporal (modelo ONNX). Ele:
- Mostra overlay de pontos faciais em tempo real (CameraX)
- Emite alertas (Microsleep e Bocejo) e exibe aviso de ÔÇ£Sinais de sonoÔÇØ
- Registra eventos no Firestore e apresenta relat├│rios di├írios
- Mant├®m sess├úo de login persistida e funciona offline ap├│s o primeiro login

## Recursos
- CameraX (frontal) com processamento cont├¡nuo
- MediaPipe Face Landmarker (landmarks 3D/precisos)
- ONNX Runtime para classifica├º├úo temporal (Microsleep/Bocejo/Atento)
- Alertas sonoros (loops/bipes) via `AlertManager`
- Relat├│rios (Pizza/Barras/Radar) com legendas que n├úo se sobrep├Áem (wordÔÇæwrap)
- Firestore com cache/persist├¬ncia offline habilitado

## Arquitetura (alto n├¡vel)
```
CameraX -> Frames -> MediaPipe FaceLandmarker -> Landmarks
       -> Extra├º├úo de features (normaliza├º├úo + pontos) -> Buffer temporal (SEQ_LEN)
       -> ONNX Runtime (LSTM) -> Decis├úo (microsleep/bocejo/atento)
       -> UI (overlay + status + contadores) -> Alertas -> Firestore (events)
```

## Avalia├º├úo e M├®tricas

### Par├ómetros alternativos

| N├¡vel de sensibilidade | Limiar Microsleep (ms olhos fechados) | Bocejos consecutivos | Aplica├º├úo sugerida |
| --- | --- | --- | --- |
| **Alta** | 1.200 ms | 2 | Turnos noturnos/frotas com hist├│rico de fadiga. Maior chance de falsos positivos. |
| **M├®dia (default)** | 2.000 ms | 3 | Situa├º├Áes gerais, equil├¡brio entre alerta e conforto do motorista. |
| **Baixa** | 2.800 ms | 4 | Testes iniciais ou motoristas que reclamam de excesso de avisos. |

Outro par├ómetro relevante:
- EAR/MAR adaptativos via hist├│rico (`HISTORY_SIZE = 5`) reduzem oscila├º├Áes do LSTM em faces parcialmente vis├¡veis.

### Lat├¬ncia no dispositivo
- A tela de an├ílise exibe a lat├¬ncia m├®dia do pipeline (MediaPipe + featuriza├º├úo + decis├úo) usando uma m├®dia exponencial (20% das amostras mais recentes).  
- O valor mostrado tamb├®m inclui o FPS efetivo (`fps = 1000/lat├¬ncia`), possibilitando comparar diferentes aparelhos antes de implantar em campo.  

## Estrutura do Projeto
```
app/
  src/main/java/com/example/faceanalysis/
    AnalysisActivity.kt ┬À AlertManager.kt ┬À OverlayView.kt
    LoginActivity.kt ┬À RegisterActivity.kt ┬À HomeActivity.kt ┬À MainActivity.kt
    ReportActivity.kt ┬À ProfileActivity.kt ┬À PrivacyActivity.kt
  src/main/res/ (layouts, values, raw)
  src/main/assets/ (face_landmarker.task, model_lstm_*.onnx)
  AndroidManifest.xml
```

## Instala├º├úo
1) Pr├®ÔÇærequisitos
- Android Studio (Giraffe+), JDK 17
- Dispositivo real com c├ómera frontal (recomendado)

2) Firebase
- Adicione seu arquivo `app/google-services.json` (n├úo ├® versionado neste reposit├│rio p├║blico)
- Habilite Auth por EÔÇæmail/Senha no Firebase Console

3) Build & Run
- Sincronize o Gradle e execute o m├│dulo `app` em um dispositivo real

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
- Sess├úo persistida (autoÔÇælogin) e Firestore offline habilitado
- Overlay espelhado para c├ómera frontal e PreviewView `fillCenter` para melhor alinhamento
- O arquivo `app/google-services.json` n├úo deve ser versionado (j├í ignorado no `.gitignore`)

## Troubleshooting
- PERMISSION_DENIED no relat├│rio: confira as regras acima e se o `uid` do documento em `users/{uid}` ├® o mesmo do usu├írio autenticado
- ÔÇ£No chart data availableÔÇØ com eventos: verifique se o dia possui eventos ativos (Microsleep/Bocejo/Atento/Sem Rosto)
- Build com pouca mem├│ria (Windows):
  ```
  ./gradlew.bat --stop
  ./gradlew.bat clean :app:assembleDebug --no-daemon
  ```

## Licen├ºa
Defina a licen├ºa do projeto (MIT/Apache 2.0, etc.) conforme sua necessidade.
