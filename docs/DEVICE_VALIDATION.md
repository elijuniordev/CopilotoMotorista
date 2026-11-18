# Validação em aparelhos reais — Copiloto Motorista

## Objetivo e status

Este documento define a validação de campo necessária antes de liberar o aplicativo para motoristas. Os testes de CI e emulador não comprovam compatibilidade com as interfaces reais do Uber Driver e 99 Motorista.

**Status em 18/11/2026:** testes unitários e builds Debug/Release aprovados no CI para API 26, 30, 35 e 36; testes instrumentados Room aprovados em emuladores API 26 e 35. A PR #33 também passou nos seis jobs automatizados após implementar exclusão manual de ofertas e limpeza restrita das evidências associadas. APKs Debug/Release são publicados como artefato temporário do CI. Testes físicos continuam pendentes. Não registrar um item como aprovado sem evidência (modelo do aparelho, versão Android, versão do app de transporte, data, resultado e observações).

## Matriz de dispositivos

| Ambiente | Finalidade | Resultado |
|---|---|---|
| Android API 26 (Android 8) | Compatibilidade mínima, overlay e árvore de acessibilidade | Pendente em aparelho físico |
| Android API 30 (Android 11) | Screenshot API e OCR local | Pendente em aparelho físico |
| Android API 35 (Android 15) | Compatibilidade recente | Pendente em aparelho físico |
| Android API 36 (Android 16) | Versão-alvo atual do projeto | Pendente em aparelho físico |

O CI compila e executa testes unitários nas quatro APIs. Os testes instrumentados Room rodam em emuladores API 26 e 35. O workflow publica APKs Debug e Release do build API 35 por 14 dias; baixar o artefato na execução aprovada do GitHub Actions. O teste de campo precisa usar pelo menos um aparelho físico por versão que se pretenda declarar como suportada.

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

- O manifesto atual não declara permissão de Internet e desativa o backup Android; confirmar essas propriedades no manifesto mesclado de cada variante de release.
- O OCR é executado localmente, mas o texto reconhecido de ofertas completas é persistido no Room em `trip_records.rawOfferText`; origem e destino extraídos também podem ser persistidos nos campos próprios. Portanto, não declarar que o texto OCR nunca é armazenado.
- Capturas de evidência podem ser gravadas em `files/trip_evidence`, no armazenamento privado do app, e associadas ao registro por `evidenceUri`. A imagem pode conter nomes, endereços ou outros dados visíveis na tela, mesmo sem ser exposta à galeria.
- Os logs atuais registram metadados e contagem de caracteres, não o conteúdo OCR bruto; revisar logs de novas alterações para manter essa regra.
- Não há envio remoto desses dados implementado no fluxo revisado; confirmar dependências, manifesto mesclado e tráfego de rede antes de cada release.
- Confirmar que eventos de pacotes não permitidos são descartados antes de iniciar a captura.
- Verificar que revogar permissões interrompe a captura e remove o overlay.
- O histórico agora oferece exclusão manual de ofertas. O repositório remove a captura associada somente quando o caminho corresponde a um JPEG gerado diretamente em `files/trip_evidence`; caminhos externos ou inválidos são recusados e o registro é preservado. Essa lógica está coberta por testes unitários, mas ainda precisa de validação em aparelho real.
- A política de retenção automática/temporal e a política de privacidade publicada continuam pendentes antes da publicação.
- Usar dados de teste controlados. Não anexar evidências com dados pessoais; mascarar informações antes de compartilhar qualquer captura fora do aparelho.

## Evidências a anexar

Para cada execução, registrar: ID do caso, resultado (aprovado/reprovado/bloqueado), aparelho, Android/build, versão do app de transporte, commit/APK, data/hora, observações e evidência sem dados pessoais. Remover ou mascarar qualquer informação de passageiro antes de anexar capturas.

## Limitações conhecidas

- OCR local por screenshot depende de Android 11/API 30 ou superior.
- Os identificadores de pacote Uber/99 podem mudar; confirmar em cada distribuição instalada.
- O parser depende da linguagem/layout textual observado; atualizações dos apps podem alterar a tela.
- O CI não simula integralmente overlays de outros apps, políticas OEM de bateria ou telas reais de ofertas.
- O histórico preserva o custo operacional total e o nome do veículo no momento da confirmação; atualizações posteriores da corrida mantêm esse snapshot.
