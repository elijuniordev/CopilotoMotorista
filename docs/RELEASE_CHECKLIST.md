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
- [ ] Concluir revisão das políticas do Google Play para AccessibilityService e permissões especiais/overlay; preparar declaração de acessibilidade, divulgação destacada e consentimento no Play Console.
- [x] Criar identidade vetorial Copiloto Pro, ícone adaptativo Android e splash screen (Bloco 12).
- [x] Refinar navegação inferior, cabeçalho e espaçamento das áreas seguras (Bloco 12).
- [ ] Produzir screenshots finais, descrição, classificação etária e ficha da loja.
- [x] Configurar assinatura de release via secrets do CI, sem armazenar keystore/senhas no repositório; CI verificou a assinatura do APK Release (esquema v2).
- [x] Gerar AAB Release assinado e verificar sua assinatura no CI (commit `cfc4954`); artefato temporário publicado junto dos APKs.
- [ ] Validar instalação/atualização do AAB em aparelho; a estratégia técnica de versionamento já está documentada abaixo.
- [x] Adicionar auditoria estática de dependências Release e referências a APIs de rede; no commit `251afeb`, a verificação CI passou sem candidatos conhecidos.
- [x] Verificar no CI o manifesto Release mesclado: sem permissão INTERNET, backup desativado e regras de extração explícitas.
- [ ] Fazer revisão final de segurança, dependências, licenças, tráfego em tempo de execução e dados pessoais; a auditoria estática não prova ausência de tráfego.

## Versionamento

A configuração atual inicia em `versionCode = 1` e `versionName = "1.0"`. Antes de cada distribuição, incrementar `versionCode` de forma monotônica; definir `versionName` conforme a versão pública aprovada. Não reutilizar o mesmo versionCode para atualizações. **A regra está definida; o incremento deve ocorrer somente quando uma nova distribuição for preparada.**

## Validações técnicas já automatizadas

- [x] Testes unitários no CI.
- [x] Build Debug e Release com minificação.
- [x] Teste instrumentado de persistência Room e chaves estrangeiras em emuladores API 26 e 35.
- [x] Testes unitários e builds Debug/Release em API 26, 30, 35 e 36.
- [x] Geração do AAB Release e verificação da assinatura com `jarsigner` no CI.
- [ ] Testes físicos conforme docs/DEVICE_VALIDATION.md.

## Restrições de release

O CI gerou o AAB Release e verificou sua assinatura com `jarsigner`; isso comprova a assinatura do artefato produzido naquele commit, mas não comprova instalação/atualização em aparelho, conformidade final com políticas, nem aprovação no Play Console. A configuração atual usa `targetSdk = 36`, nível exigido para novos apps e atualizações de apps móveis enviados ao Google Play desde 31 de agosto de 2026. Não publicar nem declarar o aplicativo pronto para produção enquanto os bloqueadores acima permanecerem.
