# Bloco 12 — Identidade visual e acabamento do Copiloto Pro

## Direção de marca aprovada
- Nome comercial: Copiloto Pro.
- Símbolo minimalista de navegação: rota azul com ponto de origem e marcador de destino verde.
- Paleta oficial: azul-marinho #0B1220, azul #3B82F6, verde #22C55E, amarelo #FACC15 e vermelho #EF4444.
- Arquivos vetoriais: `assets/brand/copiloto-pro-mark.svg` e `assets/brand/copiloto-pro-wordmark.svg`.
- Tipografia desejada: Sora. A família sans-serif do Android permanece como fallback temporário porque o arquivo de fonte local ainda não foi incorporado.

## Aplicação Android
- Ícone adaptativo com foreground, background e camada monocromática alinhados ao símbolo da marca.
- Tema Material 3 escuro com superfícies azul-marinho e cores semânticas alinhadas à paleta oficial.
- Azul reservado à identidade e às ações principais; verde para resultados positivos; amarelo para atenção; vermelho para estados críticos.
- Navegação principal preserva os seis destinos funcionais: Painel, Corridas, Agenda, Gastos, Relatórios e Veículos.
- Cabeçalho global com símbolo e wordmark Copiloto Pro.

## Escopo visual
- Preservar as regras de negócio, Room, captura, OCR, overlay e cálculos financeiros.
- Refinar navegação e hierarquia visual sem remover funcionalidades.
- Manter o card de oferta compacto com indicadores de viabilidade e informações essenciais.

## Validação
- Build Debug/Release, testes unitários na matriz Android CI e instrumentação Room em API 26 e API 35.
- Inspeção visual final em aparelho físico continua pendente.
- O Bloco 12 só será encerrado após conferência do ícone, tipografia, telas e card flutuante no aparelho.
