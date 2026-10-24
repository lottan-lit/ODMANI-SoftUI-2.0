# Implementação Soft UI 2.0

## Componentes

`ui/SoftComponents.kt`: SoftCard, SoftButton, SoftIconButton, ReadingProgress, LibrarySection, ReadingStatsCard, EmptyLibraryState, PersonalBookCard, SoftHome, NativeBookDetails e SoftStats. `PdfCover` continua sendo o componente de capa original com proporção Fit e cache limitado. `VisualTokens` centraliza cores, tipografia, espaçamentos e elevação.

## Arquivos alterados

- `android/app/build.gradle.kts`: versão 2.1.0-alpha01, código 4; sem novas dependências.
- `data/Database.kt`, esquema Room 2 e `LottanApplication.kt`: origem pessoal padrão e migração 1→2.
- `data/LibraryRepository.kt` e `ui/LibraryViewModel.kt`: releitura que preserva conclusão e histórico.
- `ui/LottanApp.kt`: Home, detalhes reais, estatísticas, rotas e identidade AUDMANI.
- `ui/LibraryScreens.kt`: filtros Todos/Pessoais/Lottan e estados vazios; livros ilustrativos removidos da biblioteca real.
- `ui/Design.kt`, `VisualTokens.kt`: cartões Soft UI e botões compartilhados; seletores em cápsula. A função BottomNav permanece idêntica à alpha03.
- `ui/SettingsScreens.kt`: tema na aparência, controles reais e diferenciação explícita entre texto ilustrativo e PDF.
- `drawable/odmani_launcher_foreground.xml`, `mipmap-anydpi/ic_launcher.xml`: inset de 6% por lado, redução visual de 12% sem editar o bitmap original.
- `BookOriginTest.kt`, `MigrationTest.kt`: origem confiável e preservação de dados.
- `web/`: página institucional AUDMANI responsiva, temas persistidos, download bloqueado e informações de dados.

## Preservado

Renderizador, gestos, zoom, rotação, banco e preferências legados, SAF, pesquisa por nome, organização virtual, cache de capas e contagem real do tempo em primeiro plano. Nenhuma permissão, serviço externo ou dependência adicionada.

O app continua com largura confortável centralizada em telas maiores. Conteúdo rolável fica fora da área reservada à navegação. O cartão de retomada muda de composição em largura reduzida ou fontes ampliadas. Fontes nativas Android são mantidas; não há arquivo Inter embarcado.
