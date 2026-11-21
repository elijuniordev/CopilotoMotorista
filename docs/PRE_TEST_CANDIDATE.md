# Copiloto Pro — Porta de entrada para testes de campo

Versão da orientação: 21/11/2026. **Este documento cobre a versão candidata para testar, não a publicação na Play Store.**
Branch de trabalho: `fix/overlay-stability-20261121`; PR #35 sobre a PR #34. **Não fazer merge sem autorização.**

## Quando a versão é considerada pronta para testar
- [ ] A execução de **CI do commit mais recente** concluiu com **6/6 jobs aprovados** (build/unitários em API 26/30/35/36 e Room instrumentado em 26/35).
- [ ] Existem APKs Debug/Release gerados por esse mesmo CI, com assinatura verificada quando aplicável.
- [ ] Não há defeitos críticos conhecidos que causem crash, capturem outras telas ou inventem dados financeiros.
- [ ] As funções acessíveis sem Uber/99 foram revisadas: cadastro/seleção de veículos, metas e custos, início/fim do expediente, histórico, despesas, relatórios, agenda e permissões.
- [ ] Nenhum dado fictício é exibido como resultado real; aceite de corrida é **registro manual dentro do Copiloto**, não um clique no Uber/99.
- [ ] O roteiro abaixo está preparado para obter evidência de falhas sem guardar dados pessoais no repositório.

**Importante:** passar no CI aprova testes automatizados, não o comportamento na tela real do Uber/99. Falhas verificadas no celular devem voltar como correção em branch antes de publicação.

## Obter o APK de teste
1. Abrir a [PR #35](https://github.com/elijuniordev/CopilotoMotorista/pull/35) e localizar a execução mais recente de **Android CI**.
2. Verificar que todos os seis jobs foram concluídos com sucesso **no HEAD** da PR; não reutilizar um resultado de commit antigo.
3. Na página da execução, baixar o artefato `copiloto-motorista-test-<sha-da-branch>` (nos commits anteriores ao ajuste de empacotamento, o artefato se chamava `copiloto-motorista-apks-<sha-do-build>`). Pode ser necessário entrar no GitHub. Os artefatos expiram depois de 14 dias.
4. Extrair o ZIP e consultar `BUILD_INFO.txt`: `source_commit` deve ser igual ao SHA atual da PR. O campo `checkout_commit` pode ser diferente porque o GitHub testa o commit de merge temporário de uma pull request. Não reutilizar APK de execução anterior.
5. Conferir a integridade dos arquivos com `SHA256SUMS.txt` sempre que possível. O ZIP contém `CopilotoPro-debug.apk`; quando os secrets de assinatura estão disponíveis e as verificações foram aprovadas, também contém `CopilotoPro-release-signed.apk` e o AAB Release assinado. **Nunca instalar um Release não assinado**; o AAB não é instalável diretamente.
6. Registrar o SHA da fonte, tipo do APK e data antes de instalar ou atualizar. Não distribuir esse APK como produto final. O Release assinado é preferível para avaliação de uso normal; o Debug serve à investigação de erros.
7. Instalar somente em dispositivo de teste autorizado, ler as divulgações de acessibilidade e sobreposição e ativar conscientemente as permissões necessárias. Em Android 8–10, autorizar MediaProjection quando solicitado.

**Verificação no computador:** após extrair, executar `sha256sum -c SHA256SUMS.txt` na pasta do pacote. No Windows, usar uma ferramenta de cálculo SHA-256 confiável ou `Get-FileHash` e comparar os hashes. No Android, guardar a identificação do pacote e do commit no relatório do teste; o download direto do GitHub não substitui a checagem de integridade.

## Validação automatizada prévia
- O CI de 21/11/2026, [execução #44917060453](https://github.com/elijuniordev/CopilotoMotorista/actions/runs/44917060453), aprovou **6 de 6 jobs** no SHA `8bf56f363b408befa3561d84f45ff10fecf316c1`; o pacote de testes desse commit foi gerado com sucesso.
- Os testes instrumentados de emulador seguintes foram ampliados para verificar a **abertura da configuração inicial**, a **exibição da divulgação sobre captura**, a **navegação para o painel financeiro sem conceder permissão** e a **preservação do painel após recriar a Activity** (rotação/recuperação de tela). Essas verificações só podem ser marcadas como aprovadas depois de CI 6/6 no novo SHA.
- Testes do emulador validam interfaces locais; não simulam o comportamento de OCR sobre Uber Driver/99 Motorista, permissão especial concedida em aparelho, sobreposição do fabricante ou consumo da bateria.

## Roteiro mínimo para a primeira rodada

| ID | Teste | Resultado obrigatório |
|---|---|---|
| TST-01 | Abrir app, cadastrar e selecionar veículo com custos e metas | Dados preservados após fechar/reabrir |
| TST-02 | Iniciar, pausar, retomar e finalizar expediente | Sem controles órfãos ou monitoramento quando inativo |
| TST-03 | Alternar entre Uber/99, Copiloto e navegador durante OCR | Nenhuma análise de outro app nem imagem de outro app vinculada a corrida |
| TST-04 | Múltiplos eventos da **mesma** oferta | Sem perdas inesperadas, piscar ou duplicidade |
| TST-05 | Uma oferta completa de Uber e uma de 99 | Valores, km, minutos e estimativas plausíveis; sem aceitar automaticamente |
| TST-06 | Radar com dois ou mais cartões e Radar parcialmente visível | Não combinar tarifas/rotas; não transformar Radar parcial em oferta única |
| TST-07 | Bloquear/desbloquear, girar tela e revogar sobreposição | Sem crash, janelas órfãs ou snapshot antigo |
| TST-08 | Marcar corrida como aceita/concluída e revisar histórico | Estados coerentes; valores de oferta não viram recebimento manual automaticamente |
| TST-09 | Excluir corrida com evidência privada | Remoção segura e comportamento previsível quando arquivo falha |
| TST-10 | Lançar despesas, recebimentos e visualizar relatório | Gráficos e saldo somente com dados registrados; semana do calendário correta |
| TST-11 | Alternar captura Android 8–10 (MediaProjection) | Permissão, captura, rotação, interrupção e retomada corretas |

A matriz completa de compatibilidade, privacidade e bateria está em `docs/DEVICE_VALIDATION.md`. Não usar nomes, endereços, telas de passageiros ou informações financeiras reais em issues públicas.

## Modelo de resultado
- ID do teste:
- Android / fabricante / modelo:
- Versão Uber Driver ou 99 Motorista (quando relevante):
- Commit e tipo de APK:
- Resultado: aprovado / reprovado / bloqueado
- Passos para reproduzir:
- Comportamento esperado e observado:
- Evidência redigida (sem informação pessoal):
- Gravidade e próximo responsável:

## Critérios de pausa
**Interromper imediatamente o ensaio** se houver captura de tela de aplicativo não autorizado, gravação de dados pessoais fora do armazenamento privado, crash em loop ou aceite de corrida não autorizado. Registrar apenas metadados não sensíveis e reabrir o ciclo de correção.

## Fora do escopo deste marco
Publicação na Play Store, política pública final, ficha da loja, aprovação de permissões pela Google, métricas comprovadas de bateria/latência em operação e cobertura de todos os modelos físicos do mercado são objetivos posteriores ao início dos testes.
