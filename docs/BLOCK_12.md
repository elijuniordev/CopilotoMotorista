# Bloco 12 — Identidade visual e acabamento do Copiloto Pro

## Direção de marca
- Nome comercial: Copiloto Pro.
- Símbolo vetorial minimalista de rota/avanço integrado a um traço em forma de C.
- Assinatura: ciano #38CFF4 e verde #31D18A sobre azul-marinho #0B1326.
- A paleta funcional do Cockpit Financeiro foi preservada para manter a semântica existente de resultado, alerta e despesa.
- Arquivos-fonte: `assets/brand/copiloto-pro-mark.svg` e `assets/brand/copiloto-pro-wordmark.svg`.

## Aplicação Android
- Nome do aplicativo atualizado para Copiloto Pro.
- Ícone adaptativo com foreground, background e camada monocromática.
- Splash screen nativa Android 12+ com fundo azul-marinho e símbolo da marca.
- Navegação principal Material 3 com ícones vetoriais, rótulos persistentes e estados selecionados.
- Cabeçalho global com símbolo e wordmark Copiloto Pro.
- Tela inicial de permissões respeita as áreas seguras do sistema.
- Os seis destinos funcionais existentes foram preservados.

## Escopo visual
- Manter o tema escuro e os tokens funcionais existentes.
- Não alterar regras de negócio, Room, captura, OCR, overlay ou cálculos financeiros.
- Refinar espaçamento estrutural superior e navegação inferior sem remover funcionalidades.

## Validação
- Build Debug/Release e testes unitários na matriz Android CI.
- Instrumentação Room em API 26 e API 35.
- Inspeção visual final em aparelho físico continua pendente e será feita com o APK gerado pelo CI.
