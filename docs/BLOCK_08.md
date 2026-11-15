# Bloco 8 — Histórico e ciclo de vida das corridas

## Implementado
- Histórico conectado ao repositório Room, com filtros por todas, aceitas, concluídas e canceladas.
- Corridas aceitas podem ser marcadas como concluídas pelo motorista.
- A transição aceita → concluída preserva o ID, horário original e demais dados; repetir a conclusão não altera o registro.
- O custo operacional estimado e o nome do veículo são armazenados como fotografia histórica no momento da confirmação.
- Banco atualizado para a versão 4 com migração 3 → 4 adicionando os campos históricos sem apagar registros.
- O fluxo de captura salva os mesmos campos históricos e impede que uma confirmação aceita atrasada reverta uma corrida já concluída.

## Limitações mantidas
- A captura de Uber/99 continua dependendo de validação em aparelhos reais.
- O histórico exibe dados efetivamente disponíveis; origem/destino, avaliações e comprovantes não são inventados.
- A edição completa dos dados da corrida e a deduplicação entre eventos independentes de captura permanecem fora deste bloco.

## Validação
A execução única do workflow Android CI compila e testa APIs 26, 30, 35 e 36; depois executa testes instrumentados Room em APIs 26 e 35. A conclusão do bloco depende do resultado real dessa execução.
