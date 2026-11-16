# Exportar e analisar exemplos de corridas do Dropbox

Este utilitário usa o GitHub Actions para baixar em lote os arquivos de uma única pasta do Dropbox, criar um ZIP criptografado e executar OCR nas imagens. O relatório de texto OCR é enviado diretamente para a mesma pasta do Dropbox para que possa ser consultado separadamente. **Os arquivos originais não são movidos, renomeados, editados ou excluídos.**

> O repositório CopilotoMotorista é público. Por isso, o GitHub Actions publica somente o ZIP criptografado (.enc), nunca o ZIP aberto nem o texto reconhecido. O relatório OCR é enviado diretamente à conta Dropbox autorizada. A chave de criptografia fica em um secret do GitHub e não é incluída no artefato.

## 1. Criar/configurar o aplicativo Dropbox

1. Acesse https://www.dropbox.com/developers/apps e use o aplicativo já criado.
2. Escolha **Scoped access** e **Full Dropbox**.
3. Na aba **Permissions**, habilite:
   - `files.metadata.read`
   - `files.content.read`
   - `files.content.write`
4. Salve as permissões. A App key e o App secret permanecem os mesmos.

A permissão de escrita é usada exclusivamente para criar o arquivo TXT do relatório OCR na pasta de exemplos. O workflow não altera os arquivos de imagem originais.

## 2. Autorizar novamente e obter o refresh token

Sempre que adicionar uma permissão ao aplicativo, gere um novo refresh token:

1. Monte este endereço, substituindo `APP_KEY` pela App key do aplicativo, e abra-o no navegador em que está conectado ao Dropbox:

   `https://www.dropbox.com/oauth2/authorize?client_id=APP_KEY&response_type=code&token_access_type=offline&scope=files.metadata.read%20files.content.read%20files.content.write`

2. Confira as permissões solicitadas e clique em **Allow/Permitir**. O Dropbox exibirá um código temporário e de uso único.
3. No computador, execute `python scripts/dropbox_oauth_setup.py` a partir da branch deste recurso.
4. Informe a mesma App key, o App secret e o código de autorização. O script troca o código por um refresh token.
5. Atualize somente o secret `DROPBOX_REFRESH_TOKEN` no GitHub. Os outros secrets permanecem iguais.

Nunca envie tokens, códigos ou segredos pelo chat nem os salve no código.

## 3. Secrets do GitHub

Em **Settings → Secrets and variables → Actions**, mantenha estes quatro secrets:

| Nome | Valor |
| --- | --- |
| `DROPBOX_APP_KEY` | App key do app Dropbox |
| `DROPBOX_APP_SECRET` | App secret do app Dropbox |
| `DROPBOX_REFRESH_TOKEN` | Refresh token com as três permissões acima |
| `EXPORT_ENCRYPTION_KEY` | Chave Fernet usada para criptografar o ZIP |

Para gerar a chave Fernet pela primeira vez:

```bash
python -m pip install cryptography
python -c "from cryptography.fernet import Fernet; print(Fernet.generate_key().decode())"
```

Se ela já estiver cadastrada, não é necessário gerar outra.

## 4. Executar o processamento

1. Abra a aba **Actions** do repositório.
2. Selecione **Export Dropbox corrida examples**.
3. Clique em **Run workflow** e mantenha o caminho sugerido: `/Copiloto Pro — Exemplos de Corridas`.
4. Aguarde a execução terminar com sucesso.

O workflow:
- baixa os arquivos da pasta para armazenamento temporário;
- cria o ZIP e executa OCR nas imagens compatíveis usando português e inglês;
- envia um arquivo `OCR_Corridas_AAAAMMDD_HHMMSS.txt` para a mesma pasta do Dropbox;
- criptografa o ZIP e publica somente o artefato criptografado no GitHub Actions, com retenção de 1 dia.

O conteúdo reconhecido não é impresso nos logs do GitHub nem publicado como artefato.

## 5. Consultar o relatório

Depois de uma execução bem-sucedida, procure na pasta do Dropbox o arquivo mais recente com nome `OCR_Corridas_...`. Esse TXT contém uma seção por imagem, com o nome do arquivo e o texto reconhecido. O OCR pode cometer erros em números, valores, endereços e nomes; esses campos devem ser conferidos visualmente quando houver dúvida.

O ZIP criptografado permanece como cópia de segurança. Para descriptografá-lo localmente:

```bash
python -m pip install cryptography
python scripts/dropbox_decrypt_export.py copiloto-pro-exemplos-corridas.zip.enc
```

Digite a chave Fernet quando solicitada.

## Privacidade e limites

- O app precisa de leitura de metadados, leitura de conteúdo e escrita de conteúdo para enviar o relatório TXT.
- A única escrita no Dropbox é a criação do novo relatório TXT. Os originais não são alterados.
- O ZIP só é publicado no GitHub depois de criptografado; a chave não é publicada nem impressa nos logs.
- O workflow é manual e só executa quando iniciado em **Actions → Run workflow**.
- O OCR processa imagens PNG, JPG/JPEG, WEBP, BMP e TIFF, usando os idiomas português e inglês.
