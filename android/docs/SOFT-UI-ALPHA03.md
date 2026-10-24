# ODMANI — Light & Dark / alpha03

## Sistema visual

Tokens centralizados em VisualTokens.kt: fundo Light #F5F6F8, superfície #FFFFFF, secundária #ECEEF1, texto #171717, borda #E4E5E8, botão #151515. Dark: #141416, #202023, #2B2B2F, #F5F5F5, #A5A5AA, #39393E, botão #F1F1F3. O texto secundário Light usa #707070 em vez de #737373 para alcançar contraste mínimo4,5:1 no fundo gelo. Tipografia usa a fonte sans-serif nativa, não um arquivo Inter licenciado externo. Raios16dp, elevação2dp, borda1dp, inset24dp e escala de8dp. Sem blur ou gradientes fortes nos fundos/cartões. Cápsula e botão independente mantêm ações, geometria e ícones. Light indicador preto; Dark branco-gelo. Barras Android recebem o fundo do tema. Claro/Escuro/Seguir sistema continuam no DataStore existente.

## Fidelidade documental

PdfCover mostra a primeira página com ContentScale.Fit e sem colorFilter, alpha especial, inversão ou dessaturação. A área neutra ao redor da miniatura é independente do tema do documento. Apenas exemplos de demonstração continuam usando capas ilustrativas, identificadas como demonstração. Loading tem indicador discreto; título e símbolo PDF aparecem apenas em caso de erro.

ThumbnailRepository gera páginas fora da thread principal, serializadas por Mutex. Limite de384px por lado; cache LRU8MiB em memória e PNGs até32MiB em cacheDir. Chave contém UUID, tamanho, modificação e versão de cache. Falha de escrita do cache não descarta uma renderização válida. Exclusão de PDF remove as miniaturas correspondentes. Biblioteca e pesquisa passam a LazyColumn para evitar manter todas as imagens compostas. As miniaturas não geram eventos de leitura ou estatísticas.

## Integridade

Nenhuma mudança de schema Room, nome de banco, caminho dos PDFs, IDs ou chaves de preferências. Mesmas rotas, confirmação de exclusão, dados, progresso, controles de leitura e métricas. Mesmo applicationId e certificado; instalar como atualização sem desinstalar.

## Verificação e limites

Testes JVM: regras de leitura existentes, proporções e limites de miniaturas, invalidação por metadados e contraste das paletas. Testes Android de pixels originais vermelho/azul, proporção, cache e invalidação preparados para execução em Android. Sem aparelho/emulador conectado: não declarar aprovação física no Moto G84, comparação visual pixel a pixel ou teste de cores executado. O repositório público recebe somente página, APK, ícone e hash; fontes continuam em ZIP local.

Referências técnicas: https://developer.android.com/develop/ui/compose/graphics/images/customize e https://developer.android.com/reference/android/graphics/pdf/PdfRenderer.Page .

Verificação final: assembleDebug, testDebugUnitTest, assembleDebugAndroidTest e lintDebug concluídos. Onze testes JVM passaram; zero erros no lint. Schema e arquivos de dados/progresso comparados byte a byte à alpha02.

SHA-256 do APK: `6221de0ba033686b17b1f27be1ddeaabcfb96a17c3d10ec83bb1cbf1e3770ff8`
