# Copiloto Motorista

Aplicativo Android em Kotlin e Jetpack Compose para apoiar motoristas de aplicativos na avaliação financeira de ofertas, gestão do turno e corridas particulares.

> Status: base inicial em desenvolvimento. Os módulos descritos no planejamento ainda não estão todos implementados.

## Requisitos de desenvolvimento

- Android Studio compatível com Android Gradle Plugin 9.3.x
- JDK 17
- Android SDK Platform 36
- Gradle Wrapper incluído no repositório

## Compilar e testar

No Windows (PowerShell):

```powershell
.`gradlew.bat testDebugUnitTest
.`gradlew.bat assembleDebug
.`gradlew.bat assembleRelease
```

No macOS/Linux:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew assembleRelease
```

O APK de depuração será gerado em `app/build/outputs/apk/debug/`.

## Direção arquitetural

A implementação evoluirá em camadas com responsabilidades separadas:

- **Apresentação:** Jetpack Compose, telas e estado de interface.
- **Domínio:** regras de negócio, cálculo de custos e análise de ofertas.
- **Dados:** Room, DataStore, DAOs e repositórios.
- **Captura:** AccessibilityService, parser de ofertas e OCR com ML Kit.
- **Overlay:** apresentação flutuante da análise.

O aplicativo deverá suportar veículos elétricos, flex, gasolina e GNV. A leitura de status das corridas será assistiva: quando a tela não fornecer evidência confiável, o motorista será solicitado a confirmar.

## Privacidade e segurança

A captura de tela e o serviço de acessibilidade serão limitados aos aplicativos de transporte selecionados pelo motorista. Dados pessoais e evidências deverão ser minimizados, armazenados localmente e tratados com controles explícitos de retenção e exclusão.

## Integração contínua

O workflow em `.github/workflows/android-ci.yml` executa testes unitários e compila os artefatos Debug e Release em cada push para `master` e em pull requests.
