# Bloco 9 — Configuração de veículos e metas financeiras

## Entregas

- Tela funcional integrada à navegação do aplicativo, preservando a identidade visual existente.
- Cadastro, edição e exclusão de veículos.
- Tipos suportados: elétrico, gasolina, etanol/flex e GNV.
- Configuração de consumo e preço de energia/combustível.
- Custos operacionais por quilômetro: manutenção, depreciação e outros custos.
- Seleção persistente do veículo ativo, consumido pelo Dashboard e pelo serviço de captura.
- Metas financeiras editáveis: resultado excelente, bom e limite de atenção por km, além do mínimo desejado por hora.
- Validação de valores, consumo positivo e ordem coerente das metas.
- Testes unitários para validações dos modelos e cobertura de mapeamento de todos os tipos de veículo.

## Persistência e integração

Os veículos são armazenados em Room. O veículo ativo e as metas são armazenados em DataStore. A tela usa os repositórios já existentes, sem criar uma segunda fonte de verdade. O serviço de captura lê o veículo ativo e as metas salvas para analisar as ofertas.

A exclusão de um veículo ativo limpa a seleção; os registros históricos preservam seus snapshots e a relação de veículo das corridas é anulada pela chave estrangeira já configurada.

## Limites

- Valores de consumo, preços e custos precisam ser preenchidos pelo motorista conforme seu veículo e sua realidade de operação.
- O app não busca preços de combustível automaticamente.
- A tela e a persistência são cobertas por testes automatizados; ainda será necessário validar usabilidade e comportamento no aparelho físico.
- Assinatura e cobrança não fazem parte deste bloco nem são necessárias para testar as funções principais.
