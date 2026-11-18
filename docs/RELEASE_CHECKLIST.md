# Checklist de publicação — Copiloto Pro

## Bloqueadores antes de uma versão pública

- [ ] Concluir e documentar a validação física com Uber Driver e 99 Motorista.
- [ ] Validar compatibilidade nos aparelhos/versões Android que serão declarados suportados.
- [ ] Resolver todos os defeitos críticos de captura, OCR, overlay, ciclo de vida e persistência.
- [x] Implementar cadastro/edição/exclusão de veículos, custos operacionais, seleção do veículo ativo e metas financeiras (Bloco 9).
- [x] Implementar agenda particular com cadastro/edição, clientes, estados do compromisso e controle de pagamentos (Bloco 10).
- [x] Integrar despesas registradas ao Dashboard e relatórios com limites financeiros claramente identificados (Bloco 11).
- [x] Finalizar despesas e relatórios funcionais (Bloco 11); histórico, veículos, agenda e despesas ainda precisam de validação em aparelho real.
- [x] Implementar edição de corrida, transição aceita → concluída e deduplicação persistente de ofertas confirmadas recentes (Bloco 8).
- [ ] Definir política de retenção dos dados locais e avaliar exportação.
- [x] Disponibilizar exclusão manual de ofertas no histórico, com remoção restrita da evidência associada ao diretório privado gerenciado (PR #33); validar o fluxo em aparelho real.
- [ ] Publicar política de privacidade acessível ao usuário, informando que texto OCR, origem/destino e capturas de evidência podem ser armazenados localmente; revisar declarações de dados/permissões da Play Store.
- [x] Descrever no próprio serviço de acessibilidade o monitoramento de eventos, os apps permitidos, o OCR local, o armazenamento local possível e como interromper a captura.
- [ ] Revisar as políticas do Google Play para AccessibilityService e overlays, incluindo divulgação destacada e consentimento.
- [x] Criar identidade vetorial Copiloto Pro, ícone adaptativo Android e splash screen (Bloco 12).
- [x] Refinar navegação inferior, cabeçalho e espaçamento das áreas seguras (Bloco 12).
- [ ] Produzir screenshots finais, descrição, classificação etária e ficha da loja.
- [x] Configurar assinatura de release via secrets do CI, sem armazenar keystore/senhas no repositório; CI verificou a assinatura do APK Release (esquema v2).
- [x] Gerar AAB Release assinado e verificar sua assinatura no CI (commit `cfc4954`); artefato temporário publicado junto dos APKs.
- [ ] Validar instalação/atualização do AAB em aparelho e definir estratégia de versionamento para releases públicas.
- [x] Adicionar auditoria estática de dependências Release e referências a APIs de rede; no commit `251afeb`, a verificação CI passou sem candidatos conhecidos.
- [x] Verificar no CI o manifesto Release mesclado: sem permissão INTERNET, backup desativado e regras de extração explícitas.
- [ ] Fazer revisão final de segurança, dependências, licenças, tráfego em tempo de execução e dados pessoais; a auditoria estática não prova ausência de tráfego.

## Versionamento

A configuração atual inicia em `versionCode = 1` e `versionName = "1.0"`. Antes de cada distribuição, incrementar `versionCode` de forma monotônica; definir `versionName` conforme a versão pública aprovada. Não reutilizar o mesmo versionCode para atualizações.

## Validações técnicas já automatizadas

- [x] Testes unitários no CI.
- [x] Build Debug e Release com minificação.
- [x] Teste instrumentado de persistência Room e chaves estrangeiras no emulador API 35 (histórico do CI).
- [x] Testes instrumentados Room em emuladores API 26 e 35; testes unitários e builds Debug/Release em API 26, 30, 35 e 36.
- [ ] Testes físicos conforme docs/DEVICE_VALIDATION.md.

## Restrições de release

O APK Release de validação é assinado no CI e sua assinatura é verificada automaticamente; isso ainda não equivale a um AAB assinado pronto para Play Store, nem à validação de instalação/atualização em aparelho. Não publicar nem declarar o aplicativo pronto para produção enquanto os bloqueadores acima permanecerem.
