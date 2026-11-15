# Checklist de publicação — Copiloto Motorista

## Bloqueadores antes de uma versão pública

- [ ] Concluir e documentar a validação física com Uber Driver e 99 Motorista.
- [ ] Validar compatibilidade nos aparelhos/versões Android que serão declarados suportados.
- [ ] Resolver todos os defeitos críticos de captura, OCR, overlay, ciclo de vida e persistência.
- [ ] Finalizar telas funcionais de veículos/metas, histórico, agenda particular, despesas e relatórios; remover placeholders que possam sugerir funções disponíveis.
- [ ] Definir fluxo completo de edição de corrida aceita para concluída e proteção contra duplicidade entre eventos repetidos.
- [ ] Definir política de retenção, exclusão e exportação dos dados locais.
- [ ] Publicar política de privacidade acessível ao usuário e revisar declarações de dados/permissões da Play Store.
- [ ] Revisar as políticas do Google Play para AccessibilityService e overlays, incluindo divulgação destacada e consentimento.
- [ ] Criar ícone/identidade final, screenshots, descrição, classificação etária e ficha da loja.
- [ ] Configurar assinatura de release com chave protegida fora do repositório e gerar AAB assinado.
- [ ] Validar instalação/atualização do artefato assinado e estratégia de versionamento.
- [ ] Fazer revisão final de segurança, dependências, licenças e dados pessoais.

## Validações técnicas já automatizadas

- [x] Testes unitários no CI.
- [x] Build Debug e Release com minificação.
- [x] Teste instrumentado de persistência Room e chaves estrangeiras no emulador API 35 (histórico do CI).
- [ ] Matriz instrumentada API 26, 30, 35 e 36 após a ampliação do workflow.
- [ ] Testes físicos conforme docs/DEVICE_VALIDATION.md.

## Restrições de release

O artefato Release compilado pelo CI ainda não está assinado para distribuição. Não publicar nem declarar o aplicativo pronto para produção enquanto os bloqueadores acima permanecerem.
