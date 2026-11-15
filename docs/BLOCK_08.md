# Bloco 8 — Histórico e ciclo de vida das corridas

## Entregas implementadas
- Histórico conectado ao Room, com filtros por todas, aceitas, concluídas e canceladas.
- Abertura de detalhes de cada corrida e edição manual de valor recebido, distâncias, duração, origem, destino e observações.
- Transição aceita → concluída com preservação do ID, horário original, status e demais dados; repetir a conclusão é idempotente.
- Custo operacional estimado e nome do veículo salvos como fotografia histórica no momento da confirmação.
- Migração do banco da versão 3 para a 4 com teste instrumentado que verifica as colunas novas e a preservação de registros anteriores.
- Deduplicação persistente de ofertas já confirmadas: a mesma oferta, da mesma plataforma, com os mesmos valores dentro de uma janela de 2 minutos reutiliza o registro existente; corridas já concluídas não são reapresentadas como novas.
- Proteção para que uma confirmação aceita atrasada não reverta uma corrida já concluída.

## Testes adicionados
- Transição aceita → concluída e idempotência.
- Edição validada preservando ciclo de vida e snapshots históricos.
- Round-trip dos mappers com os snapshots.
- Migração 3 → 4 e preservação de dados antigos.
- Busca de oferta recente para deduplicação no DAO.

## Limitações mantidas
- A captura de Uber/99 ainda depende de validação em aparelhos reais.
- Origem, destino, avaliação e comprovantes só aparecem quando fornecidos pelo motorista ou pela fonte; dados ausentes não são inventados.
- A deduplicação persistente cobre ofertas aceitas/concluídas com os mesmos dados dentro da janela de 2 minutos; ofertas nunca confirmadas continuam protegidas pela supressão temporária em memória.

## Validação
A execução consolidada do workflow Android CI compila e testa APIs 26, 30, 35 e 36; depois executa testes instrumentados Room em APIs 26 e 35. Registrar o resultado real após a execução final.
