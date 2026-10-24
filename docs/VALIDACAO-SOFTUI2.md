# Validação — 24/10/2026

## Executado

- Gradle `assembleDebug testDebugUnitTest assembleDebugAndroidTest lintDebug`: BUILD SUCCESSFUL, 2min02s na validação final.
- 13 testes JVM: 6 de regras de leitura, 3 de miniaturas, 2 de contraste/tokens e 2 de origem dos documentos. Zero falhas ou ignorados.
- Lint: zero erros, 11 avisos (dependências deliberadamente fixadas e ausência de variante monocromática do launcher). Nenhuma atualização automática de dependência para este redesign.
- Migração SQL executada em SQLite: banco criado a partir do esquema Room 1, com livro concluído, posição, atribuições, grupo e dia de atividade. Aplicada a instrução real de migração; todos os registros preservados. Estrutura resultante comparada ao esquema Room 2, inclusive padrão de origem pessoal. Isso não substitui o teste de migração em Android.
- Testes instrumentados, inclusive migração, renderizador, cache e navegação, compilados com sucesso.
- Função `BottomNav` comparada integralmente com a alpha03: idêntica. Aplicação, banco e chaves DataStore mantidos.
- Imagem oficial comparada byte a byte com a anterior: idêntica. Escala implementada por inset Android de 6% por lado, sem redesenhar.
- APK validado pelo apksigner, mesmo certificado de desenvolvimento da alpha03: SHA-256 `7fb57436520c9305b7fcda455c560213219dc2c2ecebc3f5155430ec75364992`.
- APK: 10.969.106 bytes (~10,46 MiB), versão 2.1.0-alpha01, código 4. SHA-256 `cf04f78ccd6ae19249dd557f442546acbcdac4006d602295c05b98672c139f5d`.
- JavaScript web passou na verificação de sintaxe Node. Prévia HTTP local e navegação conferidas no navegador; temas Claro/Escuro/Sistema disponíveis, tema persistiu após recarregar, notas abrem e o download permanece desabilitado.
- Layout web observado em largura desktop e em frames de 360 e 412px. Sem cortes visíveis na composição inicial. Isso não equivale a teste em navegador físico Android.
- Identidade institucional e parceria conferidas na página. Nenhuma screenshot de referência apresentada como captura real.
- GitHub: repositório criado privado antes de alterar código. Pages indisponível no plano atual; publicação não habilitada.

## Não executado / pendências

Sem dispositivo Android, adb ou emulador disponível neste ambiente. Portanto **não foram executados** testes instrumentados, instalação por cima da alpha03, importação física, gestos, persistência em aparelho, navegação entre todas as telas, fontes ampliadas ou validação de ícones em diferentes launchers. Não há capturas reais desta nova versão; precisam ser obtidas no dispositivo.

Conferir no Moto G84: nenhum/um/muitos PDFs; título longo; temas claro/escuro/sistema; retomada e releitura; folders/categorias e pesquisa; estatísticas e mapa; controles e barras do sistema; zoom/rotação; rotação de tela; fonte Android ampliada. Verificar atualização sem desinstalar, mantendo documentos e histórico.

A migração adiciona a origem; não existe integração de catálogo editorial confiável ainda. Por isso a seção Lottan aparece vazia de forma honesta. Atribuir origem verificada exige um fluxo confiável futuro, não edição pelo título.

O leitor PDF e seus gestos foram preservados, sem reescrita. Ajustes de fonte/espaçamento continuam restritos ao texto ilustrativo, conforme aviso na interface. Não existe OCR, anotações, busca dentro do PDF ou sincronização.

O APK usa assinatura debug e é **somente para teste privado**. A publicação de produção exige assinatura, validação e aprovação. A landing page está preparada, mas não publicada. Dados empresariais, contato e política formal não foram inventados.
