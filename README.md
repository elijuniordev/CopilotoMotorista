# Copiloto Pro

Aplicativo Android em Kotlin e Jetpack Compose para apoiar motoristas de aplicativos na avaliação financeira de ofertas, acompanhamento de corridas e organização do trabalho.

## Identidade visual

A marca do aplicativo é Copiloto Pro. O símbolo vetorial e o ícone adaptativo seguem o guia mais recente: rota verde, marcações brancas e pin amarelo sobre navy. A navegação principal utiliza ícones vetoriais Material, rótulos persistentes e áreas seguras do sistema.

## Estado do projeto

O núcleo do aplicativo está implementado: motor financeiro, persistência local, captura visual acionada por AccessibilityService com OCR local como caminho principal, overlay de análise, Dashboard e histórico de corridas com edição, transição de status e deduplicação de confirmações recentes.

**O aplicativo ainda não está liberado para uso produtivo ou publicação.** A captura e a leitura precisam ser validadas em aparelhos físicos com versões atuais dos aplicativos Uber Driver e 99 Motorista. Configuração de veículos e metas financeiras (Bloco 9) e agenda particular de viagens com cadastro, edição, cancelamento, conclusão e controle de pagamentos (Bloco 10) estão implementadas. Cadastro, edição, exclusão e filtros de despesas, além de relatórios semanais e mensais baseados em registros reais, estão implementados (Bloco 11).

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

O APK de depuração será gerado em `app/build/outputs/apk/debug/`. O CI configura a assinatura de release por secrets e verifica a assinatura do APK Release e do AAB gerados. A verificação técnica da assinatura não substitui testes de instalação/atualização em aparelho nem a preparação para o Google Play.

## Arquitetura

- **Apresentação:** Jetpack Compose, telas e estado de interface.
- **Domínio:** regras de negócio, cálculo de custos e análise de ofertas.
- **Dados:** Room, DataStore, DAOs e repositórios.
- **Captura:** AccessibilityService, parser de ofertas e OCR local com ML Kit.
- **Overlay:** apresentação flutuante da análise.

A área de despesas permite cadastrar, editar e excluir lançamentos com categoria, valor, data, veículo opcional, hodômetro, quantidade e observações. Os relatórios consolidam corridas concluídas e despesas efetivamente registradas por semana ou mês; o saldo é faturamento menos despesas lançadas e não representa lucro líquido completo, pois custos operacionais estimados não registrados ficam de fora. A agenda particular permite cadastrar e editar compromissos com cliente, telefone, origem, destino, data, horário, valor combinado, distância, duração, forma de pagamento e observações; também permite concluir, cancelar, marcar pagamento recebido e excluir. Os dados são mantidos localmente em Room. O cadastro de veículos permite configurar elétrico, gasolina, etanol/flex e GNV, com consumo, preço da energia, manutenção, depreciação e outros custos por km. O veículo ativo e as metas financeiras são persistidos e utilizados pelo Dashboard e pela análise de ofertas. O motor financeiro suporta veículos elétricos, gasolina, etanol/flex e GNV. A distância considerada é busca + viagem. Não é presumido retorno vazio após o desembarque. O valor da oferta é tratado como valor recebido pelo motorista.

O aplicativo não aceita nem recusa corridas automaticamente. A confirmação de corrida é manual. Somente corridas marcadas como concluídas entram no faturamento realizado do Dashboard.

## Captura, privacidade e permissões

A captura é limitada aos pacotes configurados para Uber Driver e 99 Motorista. O OCR é executado localmente. O texto reconhecido de ofertas completas, incluindo origem/destino quando extraídos, pode ser persistido nos registros locais. Capturas de evidência podem ser armazenadas no diretório privado do app e associadas a uma corrida; não são enviadas à galeria pelo fluxo de captura. O manifesto Release remove a permissão INTERNET, verificada no CI; isso não substitui análise dinâmica de tráfego.

O serviço de acessibilidade e a permissão de sobreposição são recursos sensíveis e exigem consentimento explícito do motorista nas configurações do Android. A descrição do serviço informa sua finalidade. Backup Android está desativado para evitar cópia automática dos registros operacionais e preferências.

Os registros e eventuais evidências são mantidos localmente no armazenamento privado do app. O histórico permite exclusão manual de ofertas e da evidência associada, com validação física ainda pendente. Ainda é necessário validar em aparelhos físicos o comportamento de permissões, privacidade, consumo de bateria e compatibilidade com versões reais dos aplicativos de transporte.

## Validação de estabilidade

A branch de estabilização `fix/overlay-stability-20261121` contém correções independentes de falhas da janela flutuante, limpeza de capturas de evidência, agrupamento temporal de gráficos e clareza do registro manual de corridas. A validação automatizada dessa branch não substitui ensaios em aparelho físico; consultar `docs/DELIVERY_PLAN_2026_11_21.md`.

## CI e matriz Android

O workflow em `.github/workflows/android-ci.yml` executa builds e testes unitários para APIs 26, 30, 35 e 36; os testes instrumentados Room executam nos emuladores API 26 e 35. Isso não equivale a teste com Uber/99 em aparelho real.

A matriz automatizada não substitui os testes físicos descritos em [docs/DEVICE_VALIDATION.md](docs/DEVICE_VALIDATION.md). O checklist de distribuição está em [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md).

## Regras de produto

- Veículos: elétrico, flex, gasolina e GNV.
- Distância: busca + viagem.
- Sem retorno vazio hipotético.
- Valor da oferta: valor recebido pelo motorista.
- Sem aceite/recusa automática.
- Dados ausentes ou ambíguos não são inventados.
- Estimativas operacionais são identificadas como estimativas; saldo registrado não é apresentado como lucro líquido completo.
