# Plano de estabilização e entrega — Copiloto Pro

Data de referência: 21/11/2026. Fonte de verdade: código e CI do GitHub.
**Não mesclar nenhuma PR sem autorização explícita do responsável.**
Não marcar etapas como aprovadas apenas porque o código compila.

## Base e dependências
- Branch padrão: `master`.
- PRs #25–#34 ainda abertas no início desta revisão. A #34 tem base na #33 e agrega a identidade visual mais recente.
- Esta branch de estabilização nasce do commit `7e7757b` da PR #34. Abrir PR com base na branch da #34 para isolar apenas os novos patches.
- Integrar as PRs antigas somente após verificar o grafo completo de dependências, duplicidade de alterações e comportamento financeiro. Não alterar a `master` automaticamente.

## Ciclo A — Bloqueadores de estabilidade
- [x] Recuperar de falhas de anexação/ativação do ComposeView no controle flutuante.
- [x] Separar operações de limpeza para reduzir risco de janela e owners órfãos.
- [x] Adicionar testes de unidade para falha de ativação e de limpeza no mecanismo de overlay.
- [ ] Confirmar em aparelho físico qual janela falha e obter evidência sem dados pessoais.
- [ ] Testar abrir/fechar/reabrir, mudança de app, rotação, lock/unlock, permissão revogada e interrupção do serviço.
- [ ] Avaliar logs de travamento e corrigir apenas causas observadas.

## Ciclo B — Integridade dos dados e regras de negócio
- [x] Evitar JPEG truncado quando o codificador de evidência retorna falha.
- [x] Adicionar teste instrumentado de gravação/decodificação de evidência no diretório privado.
- [ ] Validar exclusão de evidências e registro Room, inclusive falhas parciais.
- [ ] Revisar regressões de captura, deduplicação, mudança de status e confirmação tardia.
- [ ] Confirmar regras: faturamento por lançamentos manuais nos relatórios, apenas corridas concluídas no indicador de corridas realizadas, estimativas devidamente identificadas; particulares não somadas automaticamente.

## Ciclo C — OCR, Radar e MediaProjection
- [x] Reforçar o classificador: Radar com OCR parcial e título próprio nunca é processado como oferta única; três testes JVM de regressão adicionados.
- [x] Resolver falha real de CI #44895678186: classificador contava a tarifa por quilômetro como segunda oferta; agora reutiliza o reconhecimento de tarifas principais do RadarOfferExtractor, com dois novos testes de borda. Antes do patch, 157/158 testes unitários passavam; **nova CI precisa confirmar**.
- [ ] Validar extração de dois ou mais cartões do Radar em imagens reais de Uber Driver com OCR e redigir qualquer dado pessoal antes de armazenar os exemplos.
- [x] Proteger eventos de janela do próprio Copiloto sobre Uber/99: ignorar apenas quando a janela ativa da plataforma é verificada; dois testes adicionais cobrem overlay versus tela principal.
- [x] Preservar linhas repetidas na normalização OCR para não perder cartões do Radar com mesma tarifa ou ação. Adicionar dois testes de normalização e um teste de ponta a ponta (normalização → classificação → extração).
- [x] Não misturar cartões quando o OCR omitir o título `Radar de Viagens`: múltiplos preços principais acionam classificação específica de Radar ou rejeição conservadora, com dois testes extras.
- [ ] Reunir testes de parser/classificador por plataforma e layouts de oferta reais com dados pessoais removidos.
- [x] Alinhar o processamento do OCR MediaProjection às regras de preservação de ofertas completas: eventos posteriores do mesmo app não descartam o resultado válido, enquanto a sessão de privacidade descarta trocas de aplicativo.
- [ ] Validar fisicamente a política de geração e concorrência no OCR Android 8–10 e Android 11+; preservar debounce e integridade.
- [x] Implementar em branch a proteção de contexto da [Issue #36](https://github.com/elijuniordev/CopilotoMotorista/issues/36): descartar snapshots e JPEGs quando o motorista sair de Uber/99; adicionar testes JVM.
- [x] Endurecer o caminho assíncrono das ofertas: revalidar app/janela depois de consultas suspendidas no Room e antes de anexar evidência, enriquecer registro ou mostrar card.
- [x] Bloquear eventos atrasados da Uber/99 quando a janela interativa verificada já pertence a outro app; adicionar dois testes JVM para impedir reautorização indevida.
- [ ] Validar no Android real as trocas rápidas Uber ↔ outro app ↔ Uber enquanto consultas Room, OCR e evidências estão pendentes.
- [ ] **Validar** a proteção da Issue #36 nos dois fluxos Android e com trocas reais de janela, inclusive controle flutuante ativo e retorno rápido para o mesmo app. Manter a issue aberta até essa validação.
- [x] Diagnosticar CI #44892429379: quatro builds/unitários aprovados; emuladores falharam ao iniciar por falta de KVM e recusa de `-accel off` no Android Emulator 37.3.10.
- [x] Adicionar habilitação de KVM + fallback TCG compatível ao workflow Android.
- [x] Confirmar a correção de infraestrutura no CI #44898175118 (6/6 jobs aprovados: builds/unitários 26, 30, 35, 36; Room 26 e 35).
- [ ] Confirmar **nova** execução completa do CI no HEAD mais recente, após os patches de evidência e o roteiro pré-teste.
- [ ] Validar Radar com ofertas múltiplas (não combinar cartões; sem aceite automático).
- [ ] Validar permissão, rotação, término e retomada de MediaProjection em Android 8–10.
- [ ] Medir p50/p95, reconhecimento antes de desaparecer e custo de bateria em aparelhos, sem dados pessoais nos logs.

## Ciclo D — Identidade visual Premium e relatórios
- [x] Adequar card de oferta ao navy da marca, explicitar lucro estimado e registro manual do aceite.
- [x] Corrigir o gráfico mensal para semanas reais do calendário, com testes de fuso horário e mês de seis semanas.
- [ ] Conferir fidelidade dos vetores/ícone, Montserrat, contraste e áreas seguras em diferentes tamanhos.
- [ ] Revisar formulários, histórico, agenda, despesas, veículos, fechamento e Radar sem mudar regras de negócio.
- [ ] Conferir acessibilidade de toque e rótulos das ações, inclusive cards sobrepostos.

## Ciclo E — Qualidade e distribuição
- [ ] Confirmar resultado de CI para o **HEAD mais recente** desta PR (build Debug/Release, testes unitários e instrumentados).
- [x] Criar roteiro objetivo de versão candidata a testes e retirada de APKs em `docs/PRE_TEST_CANDIDATE.md`.
- [x] Reforçar compensação da escrita de evidência: apagar JPEG e desvincular o registro se a sessão do aplicativo mudar durante a gravação.
- [ ] Revisar a versão candidata funcionalmente e completar pré-teste interno conforme o novo roteiro.
- [ ] Executar `docs/DEVICE_VALIDATION.md` com Uber e 99 reais e Android suportados.
- [ ] Revisar licença e privacidade, política pública, retenção/exclusão, Data Safety e declaração de AccessibilityService.
- [ ] Auditar dependências, permissões e comportamento runtime; concluir documentação de Play Console.
- [ ] Validar assinatura, instalação e atualização; incrementar versionCode somente na distribuição.
- [ ] Solicitar autorização expressa antes de qualquer merge ou publicação.

## Critérios de pronto
Um ciclo de código é implementado quando commitado em branch de trabalho.
Um ciclo automatizado é validado quando os respectivos jobs de CI aprovam o commit indicado.
Uma funcionalidade dependente de Uber/99 só é validada depois de teste físico documentado.
A versão só é candidata à publicação quando os bloqueadores do checklist de release estiverem fechados.
