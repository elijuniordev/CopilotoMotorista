# Validação em aparelhos reais — Copiloto Motorista

## Objetivo e status

Este documento define a validação de campo necessária antes de liberar o aplicativo para motoristas. Os testes de CI e emulador não comprovam compatibilidade com as interfaces reais do Uber Driver e 99 Motorista.

**Status inicial:** matriz automatizada em emuladores configurada; testes físicos pendentes. Não registrar um item como aprovado sem evidência (modelo do aparelho, versão Android, versão do app de transporte, data, resultado e observações).

## Matriz de dispositivos

| Ambiente | Finalidade | Resultado |
|---|---|---|
| Android API 26 (Android 8) | Compatibilidade mínima, overlay e árvore de acessibilidade | Pendente em aparelho físico |
| Android API 30 (Android 11) | Screenshot API e OCR local | Pendente em aparelho físico |
| Android API 35 (Android 15) | Compatibilidade recente | Pendente em aparelho físico |
| Android API 36 (Android 16) | Versão-alvo atual do projeto | Pendente em aparelho físico |

O CI executa testes automatizados em emuladores para essas APIs. O teste de campo precisa usar pelo menos um aparelho físico por versão que se pretenda declarar como suportada.

## Preparação

- Instalar o APK Release de teste e registrar seu hash/commit.
- Registrar fabricante, modelo, versão/build Android, patch de segurança e versão de Uber Driver/99 Motorista.
- Cadastrar um veículo e custos plausíveis; conceder sobreposição e habilitar o serviço de acessibilidade conscientemente.
- Confirmar que o motorista entende o que é lido, quando o overlay aparece e como desativar o serviço.
- Usar ofertas de teste/ambiente controlado quando disponível; não realizar aceite ou recusa automática.

## Casos funcionais — repetir para Uber Driver e 99 Motorista

| ID | Cenário | Resultado esperado |
|---|---|---|
| CAP-01 | Tela inicial, mapa ou navegação sem oferta | Nenhuma análise de oferta |
| CAP-02 | Oferta completa com busca e viagem | Overlay com valor, distância e tempo lidos; custo identificado como estimativa |
| CAP-03 | Oferta sem dados de busca | Busca considerada zero apenas quando não exibida; viagem preservada |
| CAP-04 | Texto parcial na árvore, mas oferta visível na tela | Tentativa de OCR local no Android 11+; sem inventar campos |
| CAP-05 | Dados conflitantes ou ambíguos | Não exibir decisão financeira como confiável |
| CAP-06 | Notificação, tela de configurações ou outro app | Nenhuma captura/análise fora dos pacotes permitidos |
| CAP-07 | Mesma oferta gera eventos repetidos | Overlay não pisca nem duplica registros |
| CAP-08 | Oferta com valor decimal brasileiro e ponto decimal | Valor convertido corretamente para centavos |
| CAP-09 | Confirmar aceita manualmente | Registro salvo como aceita; não entra no faturamento |
| CAP-10 | Registrar concluída manualmente | Registro salvo como concluída e entra no faturamento |
| CAP-11 | Retirar permissão de overlay ou acessibilidade | App não trava; informa a necessidade de reativação |
| CAP-12 | Girar tela, alternar apps e bloquear/desbloquear | Sem overlay órfão, travamento ou análise de captura antiga |

## Desempenho e bateria

Em cada aparelho, executar pelo menos três sessões comparáveis de 30 minutos com o serviço desativado e três com o serviço ativo, usando o mesmo padrão de uso. Registrar:

- Tempo entre a oferta aparecer e o overlay ser exibido (mediana e pior caso).
- Tempo de OCR nos casos em que a árvore de acessibilidade é insuficiente.
- CPU e memória aproximadas durante uma sessão.
- Consumo de bateria reportado pelo Android antes/depois.
- Aquecimento, travamentos, ANRs, perda de eventos e interrupções do serviço.

Não há medições físicas anexadas a este repositório; nenhum número de latência ou bateria deve ser divulgado como resultado até a coleta real. Critério mínimo: sem travamento/ANR, sem análise de tela antiga e sem consumo anormal persistente em repouso. Definir metas numéricas somente após baseline nos aparelhos escolhidos.

## Privacidade e segurança

- Verificar no manifesto que não há permissão de Internet e que backup Android está desativado.
- Confirmar que o OCR não grava screenshots, imagens ou texto capturado em arquivos/logs.
- Confirmar que eventos de pacotes não permitidos são descartados antes de ler a árvore.
- Verificar que revogar permissões interrompe a captura e remove o overlay.
- Validar que dados Room/DataStore permanecem locais e que a exclusão do app remove os dados.
- Não coletar nomes, endereços ou dados de passageiros durante os testes. Se a interface real os mostrar, confirmar que não são persistidos.

## Evidências a anexar

Para cada execução, registrar: ID do caso, resultado (aprovado/reprovado/bloqueado), aparelho, Android/build, versão do app de transporte, commit/APK, data/hora, observações e evidência sem dados pessoais. Remover ou mascarar qualquer informação de passageiro antes de anexar capturas.

## Limitações conhecidas

- OCR local por screenshot depende de Android 11/API 30 ou superior.
- Os identificadores de pacote Uber/99 podem mudar; confirmar em cada distribuição instalada.
- O parser depende da linguagem/layout textual observado; atualizações dos apps podem alterar a tela.
- O CI não simula integralmente overlays de outros apps, políticas OEM de bateria ou telas reais de ofertas.
- O registro atual de corrida não preserva snapshot dos custos do veículo no momento da oferta; alterar custos pode mudar estimativas históricas.
