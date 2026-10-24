# Publicação e distribuição — ainda não autorizadas

A landing page está pronta em `web/`. Nenhuma publicação automática é configurada.

## Limitação verificada na conta

Em 24/10/2026, Settings → Pages deste repositório privado exibiu “Upgrade or make this repository public to enable Pages”. GitHub Pages não está disponível neste repositório no plano atual. A interface também informa que restringir acesso ao próprio site exige GitHub Enterprise.

Não alterar a visibilidade do projeto para resolver isso. Alternativas, sempre mediante autorização:

1. Hospedar apenas o conteúdo estático de `web/` em um serviço apropriado, mantendo o código Android privado.
2. Criar um repositório separado exclusivamente para a página pública, sem fontes Android, dados ou artefatos privados.
3. Avaliar um plano compatível; qualquer contratação deve ser feita pelo proprietário da conta. Repositório privado não significa automaticamente site Pages privado.

## Preparação para GitHub Pages, se disponível e aprovado

Usar GitHub Actions com checkout oficial e artefato restrito a `web/`; nunca publicar a raiz do repositório. Alternativamente copiar somente os arquivos de `web/` para a raiz de uma branch de publicação escolhida. O site não requer build. Subdiretórios usam URLs relativas, compatíveis com project Pages.

## Liberar o download

1. Validar importação, PDF, retomada, migração, acessibilidade, layouts e temas em dispositivo.
2. Configurar assinatura de produção fora do Git e definir estratégia de atualização. Builds atuais usam assinatura de desenvolvimento, preservada neste ambiente, e não são aprovados para distribuição pública.
3. Gerar APK release, executar validações, obter aprovação de distribuição e calcular tamanho/SHA-256.
4. Publicar a versão aprovada no canal autorizado. Um asset de repositório privado exige autenticação: não serve de download anônimo público.
5. Substituir o botão desabilitado em `web/index.html` por um link para esse APK real, atualizar versão/tamanho/hash/notas e remover o aviso de desenvolvimento somente após aprovação.
6. Acrescentar capturas reais de ambos os temas e dados oficiais de contato/política quando fornecidos.

Não existem APKs no diretório web, analytics, formulários, chaves ou links fictícios de download. O APK local de teste deve ser compartilhado apenas no canal privado apropriado.
