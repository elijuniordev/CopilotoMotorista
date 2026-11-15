# Copiloto Motorista

Aplicativo Android em Kotlin e Jetpack Compose para apoiar motoristas de aplicativos na avaliação financeira de ofertas, acompanhamento de corridas e organização do trabalho.

## Estado do projeto

O núcleo do aplicativo está implementado: motor financeiro, persistência local, captura assistiva por AccessibilityService, OCR local de fallback, overlay de análise, Dashboard e histórico de corridas com edição, transição de status e deduplicação de confirmações recentes.

**O aplicativo ainda não está liberado para uso produtivo ou publicação.** A captura e a leitura precisam ser validadas em aparelhos físicos com versões atuais dos aplicativos Uber Driver e 99 Motorista. Agenda particular, relatórios e fluxo completo de configuração do veículo ainda têm áreas em desenvolvimento.

## Requisitos de desenvolvimento

- Android Studio compatível com Android Gradle Plugin 8.13.2
- Gradle 8.13 (via Gradle Wrapper)
- JDK 17
- Android SDK Platform 36

## Compilar e testar

No Windows (PowerShell):

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

No macOS/Linux:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew assembleRelease
```

O APK de depuração será gerado em `app/build/outputs/apk/debug/`. O artefato Release atual é compilado com minificação, mas ainda não está assinado para distribuição.

## Arquitetura

- **Apresentação:** Jetpack Compose, telas e estado de interface.
- **Domínio:** regras de negócio, cálculo de custos e análise de ofertas.
- **Dados:** Room, DataStore, DAOs e repositórios.
- **Captura:** AccessibilityService, parser de ofertas e OCR local com ML Kit.
- **Overlay:** apresentação flutuante da análise.

O motor financeiro suporta veículos elétricos, gasolina, etanol/flex e GNV. A distância considerada é busca + viagem. Não é presumido retorno vazio após o desembarque. O valor da oferta é tratado como valor recebido pelo motorista.

O aplicativo não aceita nem recusa corridas automaticamente. A confirmação de corrida é manual. Somente corridas marcadas como concluídas entram no faturamento realizado do Dashboard.

## Captura, privacidade e permissões

A captura é limitada aos pacotes configurados para Uber Driver e 99 Motorista. O OCR é executado localmente. Capturas de tela não são armazenadas pelo fluxo de OCR. O aplicativo não solicita permissão de Internet.

O serviço de acessibilidade e a permissão de sobreposição são recursos sensíveis e exigem consentimento explícito do motorista nas configurações do Android. A descrição do serviço informa sua finalidade. Backup Android está desativado para evitar cópia automática dos registros operacionais e preferências.

Os registros são mantidos localmente. Ainda é necessário validar em aparelhos físicos o comportamento de permissões, privacidade, consumo de bateria e compatibilidade com versões reais dos aplicativos de transporte.

## CI e matriz Android

O workflow em `.github/workflows/android-ci.yml` executa testes unitários, builds Debug/Release e testes instrumentados de persistência Room em emuladores API 26, 30, 35 e 36.

A matriz automatizada não substitui os testes físicos descritos em [docs/DEVICE_VALIDATION.md](docs/DEVICE_VALIDATION.md). O checklist de distribuição está em [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md).

## Regras de produto

- Veículos: elétrico, flex, gasolina e GNV.
- Distância: busca + viagem.
- Sem retorno vazio hipotético.
- Valor da oferta: valor recebido pelo motorista.
- Sem aceite/recusa automática.
- Dados ausentes ou ambíguos não são inventados.
- Estimativas operacionais são identificadas como estimativas; saldo registrado não é apresentado como lucro líquido completo.
