<div align="center">

# DriveON - Face Analysis (Android)

Análise em tempo real de fadiga do motorista usando CameraX + MediaPipe + ONNX, com alertas sonoros e histórico no Firebase.

![Android](https://img.shields.io/badge/Android-8.0%2B-brightgreen?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin)
![Gradle](https://img.shields.io/badge/Gradle-8.13-02303A?logo=gradle)
![AGP](https://img.shields.io/badge/AGP-8.12.3-3DDC84?logo=android)
![License](https://img.shields.io/badge/License-TBD-lightgrey)

</div>

## Visão Geral
O DriveON é um app Android que detecta sinais de fadiga do motorista a partir de landmarks faciais (MediaPipe) e classificação temporal (modelo ONNX). O app:
- Mostra overlay dos pontos faciais em tempo real (CameraX).
- Emite alertas sonoros (microsleep e bocejo) e diálogo de atenção.
- Registra eventos no Firestore (com métricas) e apresenta relatórios diários.
- Mantém sessão de login persistida e funciona offline após o primeiro login (cache do Firestore).

## Recursos
- CameraX (frontal) com processamento contínuo.
- MediaPipe Face Landmarker para landmarks faciais.
- Classificação temporal com ONNX Runtime (Microsleep/Bocejo/Atento).
- Alertas sonoros (loops/bipes) e aviso de “Sinais de sono”.
- Histórico + relatórios (Pizza/Barras/Radar) com legendas que não sobrepõem.

## Instalação rápida
1. Adicione seu pp/google-services.json (não versionado neste repo público).
2. Sincronize o Gradle e rode o módulo pp em um dispositivo real.

## Firestore (estrutura e regras)
`
users/{uid}/events (document)
  status: "Microsleep" | "Bocejo" | "Atento" | "Sem Rosto" | "Sinais de Sono"
  startTime, endTime, duration (ms)
  ear, mar, confidence, device
  startReadable, endReadable
`

Regras:
`
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
`

## Notas
- Sessão persistida (auto‑login) e Firestore offline habilitado.
- Overlay espelhado para câmera frontal e PreviewView illCenter para melhor alinhamento.
- README simplificado aqui; veja o código para detalhes das telas e flows.
