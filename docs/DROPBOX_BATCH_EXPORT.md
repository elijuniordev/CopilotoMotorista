# Exportar exemplos de corridas do Dropbox

Este utilitário usa o GitHub Actions para baixar em lote os arquivos de uma única pasta do Dropbox, criar um ZIP e criptografá-lo antes de publicar o artefato da execução. **Ele apenas lista e baixa arquivos do Dropbox; não move, renomeia, edita ou exclui os originais.**

> O repositório CopilotoMotorista é público. Por isso, o artefato do GitHub Actions pode ser acessível a outras pessoas. O workflow publica somente o arquivo criptografado (.enc), nunca o ZIP aberto. A chave de criptografia fica em um secret do GitHub e não é incluída no artefato.

## 1. Criar o aplicativo Dropbox

1. Acesse https://www.dropbox.com/developers/apps e crie um app.
2. Escolha **Scoped access** e **Full Dropbox** (o app precisa enxergar a pasta existente na raiz, não apenas uma nova pasta exclusiva do app).
3. Na aba **Permissions**, habilite somente:
   - `files.metadata.read`
   - `files.content.read`
4. Salve as permissões e, na aba **Settings**, copie o **App key** e o **App secret**.

O acesso é somente leitura. Não habilite permissões de escrita, exclusão, compartilhamento ou gerenciamento de membros.

## 2. Autorizar sua conta e obter o refresh token

1. Monte este endereço, substituindo `APP_KEY` pela App key do seu aplicativo, e abra-o no navegador em que você está conectado ao Dropbox:

   `https://www.dropbox.com/oauth2/authorize?client_id=APP_KEY&response_type=code&token_access_type=offline&scope=files.metadata.read%20files.content.read`

2. Confira as permissões solicitadas e clique em **Allow/Permitir**. O Dropbox exibirá um código de autorização. Esse código é temporário e de uso único.
3. No computador, execute `python scripts/dropbox_oauth_setup.py` a partir da branch deste recurso.
4. Informe a App key, o App secret e o código de autorização quando solicitado. O App secret e o código são digitados sem aparecer na tela. O script troca o código por um refresh token e mostra somente esse refresh token.
5. Copie o refresh token para o secret do GitHub na etapa seguinte. Não o envie nesta conversa, não o compartilhe e não o salve no código.

Se o Dropbox não retornar refresh token, repita a autorização usando exatamente o parâmetro `token_access_type=offline`.

## 3. Gerar a chave de criptografia

No computador, com Python instalado, execute:

```bash
python -m pip install cryptography
python -c "from cryptography.fernet import Fernet; print(Fernet.generate_key().decode())"
```

Copie a chave gerada e guarde-a em local seguro. Ela será necessária para abrir o ZIP depois. Não a envie nesta conversa nem a coloque no código.

## 4. Cadastrar os secrets no GitHub

No repositório, abra **Settings → Secrets and variables → Actions → New repository secret** e crie estes quatro secrets:

| Nome | Valor |
| --- | --- |
| `DROPBOX_APP_KEY` | App key do app Dropbox |
| `DROPBOX_APP_SECRET` | App secret do app Dropbox |
| `DROPBOX_REFRESH_TOKEN` | Refresh token obtido no passo 2 |
| `EXPORT_ENCRYPTION_KEY` | Chave Fernet gerada no passo 3 |

Não crie um secret para o caminho da pasta: o workflow solicita esse caminho ao iniciar e já sugere `/Copiloto Pro — Exemplos de Corridas`.

## 5. Executar e baixar

1. Abra a aba **Actions** do repositório.
2. Selecione **Export Dropbox corrida examples**.
3. Clique em **Run workflow** e mantenha o caminho sugerido.
4. Aguarde a execução terminar com sucesso.
5. Na página da execução, baixe o artefato `copiloto-pro-exemplos-corridas-encrypted`.

O artefato criptografado fica disponível por 1 dia. Ele não pode ser aberto sem a chave guardada no secret.

## 6. Descriptografar o ZIP

No computador, com o arquivo `.enc` baixado e o projeto disponível, execute:

```bash
python -m pip install cryptography
python scripts/dropbox_decrypt_export.py copiloto-pro-exemplos-corridas.zip.enc
```

Digite a chave de criptografia quando solicitada. O script gera `copiloto-pro-exemplos-corridas.zip`. Anexe esse ZIP nesta conversa para analisar os prints em conjunto.

## Privacidade e limites

- O artefato do GitHub Actions é criptografado antes do upload; a chave não é publicada nem impressa nos logs.
- O app Dropbox tem somente permissões de leitura. Os originais não são alterados.
- O workflow é manual: nada é executado automaticamente nem em intervalos recorrentes.
- Os arquivos são processados em armazenamento temporário do runner e incluídos no ZIP mantendo os caminhos relativos dentro da pasta de origem.
- A integração não anexa automaticamente o ZIP ao ChatGPT. É necessário baixar o artefato, descriptografar no computador e anexar aqui.
