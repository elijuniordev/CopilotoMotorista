# Preparação Google Play — Copiloto Pro

**Status:** documento de preparação; não representa aprovação do Google Play nem parecer jurídico. Revisado em 18/11/2026 com base nas políticas oficiais consultadas nesta data.

## Situação técnica observada no repositório

- `targetSdk = 36` e `compileSdk = 36`; para novos apps e atualizações móveis, o Google Play exige API 36 ou superior desde 31/08/2026.
- O app declara `SYSTEM_ALERT_WINDOW` para o card flutuante e usa `AccessibilityService` para detectar eventos dos apps de transporte e iniciar a captura visual.
- O serviço filtra eventos por pacote antes de solicitar screenshot. O conteúdo da oferta vem da imagem visível e do OCR local.
- O texto OCR completo, origem/destino e evidências podem ser persistidos no armazenamento privado do app.
- O usuário confirma uma divulgação em tela inicial antes de abrir o atalho das configurações de acessibilidade. O app não realiza aceite ou recusa automática de corridas.
- O manifesto remove `INTERNET` e o CI verifica o manifesto Release mesclado; isso não substitui uma revisão de tráfego em execução.

## Bloqueadores e ações no Play Console

- [ ] Criar a ficha do app e descrever claramente a função do serviço de acessibilidade, por que é necessário, quais dados são lidos e o que o app faz com eles.
- [ ] Preencher a declaração de uso da Accessibility API no Play Console com a finalidade real e o fluxo de captura/OCR. Não marcar `isAccessibilityTool=true`: o produto não tem como finalidade principal auxiliar pessoas com deficiência, conforme o escopo atual.
- [ ] Anexar/fornecer a divulgação destacada e o consentimento afirmativo dentro do app. A tela atual é uma implementação inicial; revisar se o texto deixa explícitos dados acessados, uso e compartilhamento (inclusive declarar que não há compartilhamento somente se isso for confirmado por revisão completa).
- [ ] Confirmar que o consentimento aparece no fluxo normal, antes de iniciar o acesso, e que recusar não é interpretado como consentimento nem bloqueia funções não relacionadas ao serviço.
- [ ] Revisar a permissão especial `SYSTEM_ALERT_WINDOW`: justificar a necessidade do overlay, enviar o usuário às configurações do sistema para concessão e respeitar a recusa sem pressionar ou manipular.
- [ ] Publicar uma política de privacidade em URL pública, estável e acessível, consistente com o comportamento real, antes da distribuição.
- [ ] Preencher a seção Data safety com precisão, incluindo processamento local, dados armazenados, evidências visuais e ausência/presença de compartilhamento; não inferir as respostas apenas pela ausência da permissão INTERNET.
- [ ] Confirmar formulário de classificação etária, público-alvo, anúncios (se aplicável), acesso ao app para revisão e credenciais/dados de demonstração necessários.
- [ ] Preparar screenshots reais, ícone, descrição curta/longa e informações de suporte/contato.
- [ ] Enviar primeiro para teste interno e acompanhar eventuais avisos de política/revisão antes de qualquer lançamento público.
- [ ] Arquivar a resposta da revisão de política e atualizar este documento se o Google solicitar ajustes.

## Pontos de atenção da Accessibility API

O Google Play restringe o uso da Accessibility API. Apps que não são ferramentas de acessibilidade precisam declarar esse uso no Play Console e cumprir divulgação destacada e consentimento afirmativo dentro do app. A declaração deve refletir o comportamento real e ser atualizada se o uso mudar.

A política proíbe usar a API para um agente que inicia, planeja e executa ações ou decisões autonomamente. O escopo atual deve continuar limitado a observar eventos, filtrar os apps autorizados, ler a oferta visível e exibir análise informativa; aceite, recusa e demais decisões devem permanecer sob ação do motorista. Não adicionar automação de interação ou decisão sem nova análise formal de política.

Não declarar o app como ferramenta de acessibilidade nem usar o atributo `isAccessibilityTool` para contornar a divulgação/consentimento: essa designação é reservada a apps cuja funcionalidade principal atende diretamente pessoas com deficiência.

## Permissão de sobreposição

`SYSTEM_ALERT_WINDOW` é uma permissão especial. A orientação do Google Play é encaminhar o usuário à página de configurações do sistema para concedê-la. A interface deve explicar a finalidade, respeitar a recusa e não pressionar o usuário. Testar se o app continua utilizável sem overlay e se não interfere de forma enganosa ou disruptiva com outros apps.

## Dados e política de privacidade — fatos que precisam constar

- Eventos de acessibilidade são recebidos para identificar o Uber Driver e o 99 Motorista.
- Após filtro por pacote, a captura visual é usada para OCR local.
- Texto reconhecido de ofertas, origem/destino e imagens de evidência podem ser guardados no aparelho.
- Evidências podem conter dados pessoais que apareçam na tela de transporte.
- O usuário pode excluir manualmente ofertas pelo histórico; não existe hoje política de retenção automática documentada.
- A auditoria estática de rede e a ausência de `INTERNET` no manifesto Release são evidências técnicas limitadas; não usar isso, isoladamente, para afirmar ausência absoluta de transmissão.
- Antes de finalizar a política, definir responsável legal/editor, contato de privacidade/suporte, prazo/regras de retenção, processo de exclusão e resposta a solicitações. Esses dados não devem ser inventados neste repositório.

## Referências oficiais consultadas

- Requisitos de nível da API: https://support.google.com/googleplay/android-developer/answer/11926878?hl=pt-BR
- Uso da AccessibilityService API: https://support.google.com/googleplay/android-developer/answer/10964491?hl=pt-BR
- Dados do usuário, divulgação e consentimento: https://support.google.com/googleplay/android-developer/answer/10144311?hl=pt-BR
- Permissões e APIs que acessam informações sensíveis (inclui permissões especiais): https://support.google.com/googleplay/android-developer/answer/16558241?hl=pt-BR

As políticas podem mudar. Reconsultar as páginas oficiais e a seção App content do Play Console imediatamente antes do envio.
