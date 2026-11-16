# Exportar exemplos de corridas do Dropbox

Este utilitário usa o GitHub Actions para baixar em lote os arquivos de uma única pasta do Dropbox e gerar um ZIP como artefato privado da execução. **Ele apenas lista e baixa arquivos do Dropbox; não move, renomeia, edita ou exclui os originais.**

## 1. Criar o aplicativo Dropbox

1. Acesse https://www.dropbox.com/developers/apps e crie um app.
2. Escolha **Scoped access** e **Full Dropbox** (o app precisa enxergar a pasta existente na raiz, não apenas uma nova pasta exclusiva do app).
3. Na aba **Permissions**, habilite somente:
   - `files.metadata.read`
   - `files.content.read`
4. Salve as permissões e, na aba **Settings**, obtenha o App key e o App secret.
5. Gere um **refresh token OAuth** para sua conta com o escopo `offline_access` e as duas permissões acima. Não use um access token temporário como refresh token.

> Segurança: o app deve ser somente leitura. Não habilite permissões de escrita, exclusão, compartilhamento ou gerenciamento de membros. Nunca cole App secret, refresh token ou access token nesta conversa ou no código do repositório.

## 2. Cadastrar os secrets no GitHub

No repositório, abra **Settings → Secrets and variables → Actions → New repository secret** e crie estes três secrets:

| Nome | Valor |
| --- | --- |
| `DROPBOX_APP_KEY` | App key do app Dropbox |
| `DROPBOX_APP_SECRET` | App secret do app Dropbox |
| `DROPBOX_REFRESH_TOKEN` | Refresh token OAuth |

Não crie um secret para o caminho da pasta: o workflow solicita esse caminho ao iniciar e já sugere `/Copiloto Pro — Exemplos de Corridas`.

## 3. Executar

1. Abra a aba **Actions** do repositório.
2. Selecione **Export Dropbox corrida examples**.
3. Clique em **Run workflow** e mantenha o caminho sugerido.
4. Aguarde a execução terminar com sucesso.
5. Na página da execução, baixe o artefato **copiloto-pro-exemplos-corridas**.

O artefato fica disponível por 3 dias. Depois de baixá-lo no aparelho/computador, anexe o ZIP nesta conversa para que os prints sejam analisados em conjunto.

## Privacidade e limites

- O ZIP é publicado como artefato da execução do GitHub Actions, acessível a quem tiver acesso ao repositório; não é um link público de download.
- O workflow é manual: nada é executado automaticamente nem em intervalos recorrentes.
- O token é renovado durante a execução usando os secrets do GitHub e não é impresso nos logs.
- Os arquivos são processados em memória temporária do runner e incluídos no ZIP mantendo os caminhos relativos dentro da pasta de origem.
- A integração não anexa automaticamente o ZIP ao ChatGPT. É necessário baixar o artefato e anexá-lo aqui.
