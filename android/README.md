# ODMANI Android — Soft UI 2.0

Desenvolvido pela AUDMANI. Lottan é parceira editorial. Versão 2.1.0-alpha01, versionCode 4, aplicação nativa Kotlin e Jetpack Compose.

Consulte o README na raiz e `../docs/` para instruções atuais. Os documentos antigos neste diretório registram etapas anteriores; não substituem a documentação Soft UI 2.0.

Preservados: applicationId, banco `lottan.db`, DataStore, arquivos privados, renderizador original e navegação inferior. Esquema Room 2 adiciona `origin`, com migração não destrutiva a partir da versão 1. Todos os PDFs antigos permanecem pessoais.

JDK17, SDK35, Build Tools35.0.0. Configure `local.properties` fora do Git. Compile com `./gradlew assembleDebug testDebugUnitTest assembleDebugAndroidTest lintDebug`. Testes instrumentados exigem Android conectado ou emulador: `./gradlew connectedDebugAndroidTest`.

Builds debug são somente para teste privado. Não desinstale a versão nativa anterior para atualizar. A assinatura precisa coincidir; nenhuma chave integra este projeto.
