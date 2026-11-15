# Bloco 11 — Despesas e relatórios

## Entregas
- Cadastro, edição e exclusão de despesas persistidas no Room.
- Categorias: combustível, manutenção, pedágio, alimentação, lavagem, seguro, documentação, estacionamento e outros.
- Campos: descrição, valor, data, veículo opcional, hodômetro, quantidade, unidade e observações.
- Filtro por categoria e totalizador do conjunto filtrado.
- Relatórios do período semanal (segunda-feira até hoje) e mensal (primeiro dia até hoje).
- Indicadores: faturamento de corridas concluídas, despesas registradas, saldo registrado, corridas, distância, saldo por km/hora e despesas por categoria.
- Dashboard já combina despesas do Room e apresenta despesas do dia; novos lançamentos atualizam o fluxo observado automaticamente.
- Testes unitários de período, status de corrida, totais, categorias, saldo negativo e divisões sem distância/tempo.

## Regras contábeis e limites
- Somente corridas com status `completed` entram no faturamento.
- Somente despesas efetivamente cadastradas entram nos custos do relatório.
- Saldo registrado = faturamento de corridas concluídas − despesas cadastradas.
- Custos operacionais estimados que não foram lançados como despesa não são subtraídos novamente.
- A agenda de corridas particulares não é somada neste relatório para evitar contar como receita um compromisso ainda não pago/concluído. Integração da agenda exige regra financeira explícita.
- Os períodos são calculados no fuso local do aparelho; semana começa na segunda-feira.
- A data da despesa é registrada no início do dia local informado.

## Validação
A execução do workflow Android CI deve confirmar builds, testes unitários, testes Room e publicação dos APKs. A validação em aparelho físico continua necessária para usabilidade e fluxo real.
