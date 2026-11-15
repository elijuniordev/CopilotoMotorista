# Bloco 10 — Agenda particular de viagens

## Entregas implementadas

- Tela Agenda integrada à navegação principal e aos repositórios locais existentes.
- Cadastro e edição com título, cliente, telefone/WhatsApp, origem, destino, data, horário, valor combinado, distância e duração estimadas, forma de pagamento e observações.
- Reutilização de clientes por telefone ou, quando ausente, por nome; criação/atualização local dos dados.
- Lista persistente com filtros Todas, Agendadas, Concluídas e Canceladas.
- Transições de agendada para concluída ou cancelada, sem reabrir compromissos encerrados por repetição da ação.
- Controle independente do pagamento: pendente ou recebido.
- Exclusão explícita com confirmação.
- Validação de campos, valores, distância, duração e estados permitidos.
- Testes unitários das regras de domínio e teste instrumentado Room de criação, atualização, reabertura do banco e exclusão.

## Persistência e compatibilidade

A implementação reutiliza as tabelas `private_trips` e `clients`, entidades, DAOs, mappers e repositórios já existentes. Não foi necessária alteração de schema ou migração; o banco permanece na versão 4. Os dados continuam locais no dispositivo.

## Regras de produto

- Agendamentos particulares não são confundidos com corridas capturadas do Uber/99.
- O valor combinado é registrado como valor acordado, não como receita recebida automaticamente.
- Concluir o compromisso e marcar o pagamento como recebido são ações independentes.
- A tela não envia mensagens, não realiza cobranças e não faz reservas externas.

## Validação

O workflow Android CI executa testes unitários e builds Debug/Release em APIs 26, 30, 35 e 36, além de testes instrumentados Room nos emuladores API 26 e 35. O resultado será registrado após a conclusão da Action. Testes físicos de usabilidade continuam necessários antes da publicação.
