# Checklist de publicação — Copiloto Motorista

## Bloqueadores antes de uma versão pública

- [ ] Concluir e documentar a validação física com Uber Driver e 99 Motorista.
- [ ] Validar compatibilidade nos aparelhos/versões Android que serão declarados suportados.
- [ ] Resolver todos os defeitos críticos de captura, OCR, overlay, ciclo de vida e persistência.
- [ ] Finalizar telas funcionais de veículos/metas, agenda particular, despesas e relatórios; remover placeholders que possam sugerir funções disponíveis. O histórico de corridas está implementado, mas ainda precisa de validação em aparelho real.
- [x] Implementar edição de corrida, transição aceita → concluída e deduplicação persistente de ofertas confirmadas recentes (Bloco 8).
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
- [x] Testes instrumentados Room em emuladores API 26 e 35; testes unitários e builds Debug/Release em API 26, 30, 35 e 36.
- [ ] Testes físicos conforme docs/DEVICE_VALIDATION.md.

## Restrições de release

O artefato Release compilado pelo CI ainda não está assinado para distribuição. Não publicar nem declarar o aplicativo pronto para produção enquanto os bloqueadores acima permanecerem.
