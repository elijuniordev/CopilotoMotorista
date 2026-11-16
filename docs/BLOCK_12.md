# Bloco 12 — Identidade visual e acabamento do Copiloto Pro

## Direção de marca aprovada
- Nome comercial: Copiloto Pro.
- Símbolo minimalista de navegação: rota azul com ponto de origem e marcador de destino verde.
- Paleta da marca: azul-marinho #0B1220, azul #3B82F6 e verde #22C55E.
- Cores funcionais para estados: amarelo #FACC15 e vermelho #EF4444.
- Assinatura vetorial: `assets/brand/copiloto-pro-mark.svg` e `assets/brand/copiloto-pro-wordmark.svg`.
- Tipografia de marca definida: Sora. A fonte local ainda precisa ser incorporada ao app; o SVG usa Sora com fallback sans-serif.

## Aplicação Android
- Nome do aplicativo: Copiloto Pro.
- Ícone adaptativo com foreground, background e camada monocromática alinhados ao símbolo aprovado.
- Splash screen nativa Android 12+ com fundo azul-marinho e símbolo da marca.
- Navegação principal Material 3 com ícones vetoriais, rótulos persistentes e estados selecionados.
- Cabeçalho global com símbolo e wordmark Copiloto Pro.
- Os seis destinos funcionais existentes são preservados.

## Escopo visual
- Preservar o tema escuro e os tokens funcionais do Cockpit Financeiro.
- Refinar espaçamento estrutural e navegação inferior sem remover funcionalidades.
- Não alterar regras de negócio, Room, captura, OCR, overlay ou cálculos financeiros.

## Validação
- Build Debug/Release e testes unitários na matriz Android CI.
- Instrumentação Room em API 26 e API 35.
- Inspeção visual final em aparelho físico continua pendente e será feita com o APK gerado pelo CI.
- A identidade visual não será considerada encerrada até a conferência do ícone e das telas no aparelho.
