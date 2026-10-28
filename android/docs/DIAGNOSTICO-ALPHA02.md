# Diagnóstico e implementação — 2.1.0-alpha02

## Base preservada

Aplicativo Android nativo em Kotlin, Jetpack Compose e Material 3; Android 8+ (minSdk 26), compile/target SDK 35, Java 17. Identificador `br.com.lottan.reader.v2` preservado. O módulo web anterior foi mantido no repositório, sem substituir o aplicativo nativo.

A navegação usa Navigation Compose. A função original `BottomNav`, incluindo ícones, nomes, ordem e destinos Início, Explorar, Atividade e Biblioteca, foi preservada integralmente. O Menu e suas configurações usam voltar; as telas principais mantêm acesso ao Menu.

O leitor original usa `PdfRenderer`, renderização serializada e cache limitado a 24 MiB. As miniaturas usam a primeira página real e cache de memória limitado a 8 MiB. A importação mantém uma cópia privada, com limite de 200 MB e validação pelo renderizador nativo.

## Persistência

A base Room fornecida estava na versão 2, com livros, grupos e agregados diários, e migração 1→2 existente. Foi preservada e complementada:

- 2→3 cria associações muitos-para-muitos de pastas e páginas únicas visitadas com permanência. As associações antigas em `books.folder` são copiadas para a tabela nova.
- 3→4 acrescenta sessões detalhadas, sem tentar reconstruir sessões que o histórico antigo não registrou.
- Não há migração destrutiva.
- Preferências anteriores continuam no DataStore original.
- Um DataStore adicional guarda fixados, leitura principal, metas, anotações, checklist e conforto global/individual. A escolha do modo visível não exclui notas nem tarefas.
- Capas de pastas ficam em arquivos privados separados dos PDFs, com redução de resolução e recorte proporcional. Um documento continua tendo uma única cópia física mesmo em várias pastas.

## Referências visuais examinadas

Todas as cinco imagens de OD.zip foram abertas antes das alterações:

1. Mockup do App de Leitura Odemani: Home e detalhes pessoais.
2. Menu ODMANI em tema escuro: central de preferências.
3. Comparativo da Biblioteca em Modo Escuro: estados vazio e preenchido.
4. Comparativo de Estatísticas de Leitura: indicadores, gráfico e calendário.
5. Biblioteca Lottan: Explorar em Destaque: vitrine vazia e catálogo.

As capas, os títulos, os preços e os indicadores ilustrativos não foram importados como dados reais. A barra inferior das referências não substituiu a original.

## Bugs tratados

BUG-001: a importação original não gravava dias de leitura. A distorção principal encontrada era a normalização de cada gráfico pelo maior valor, com escala mínima de apenas 1 ms: uma leitura pequena podia ocupar toda a altura. A escala mínima passou a 30 minutos ou 30 visitas, com unidade explícita e dias zerados sem barra. Renderizar uma página não registra visita imediatamente; exige permanência de cinco segundos. Páginas únicas e visitas repetidas são separadas. O tempo para após dois minutos sem interação e é interrompido ao sair do primeiro plano. Há controles de privacidade para novos registros.

BUG-002: o cabeçalho reutilizável sempre desenhava o hambúrguer, inclusive no próprio Menu. Agora os cabeçalhos internos usam voltar e o hambúrguer fica nas telas principais apropriadas; a navegação mantém `launchSingleTop`.

Outros: sobreposição no layout compacto da Home corrigida; progresso de documentos nunca abertos começa em zero; PDFs de uma página não aparecem automaticamente como concluídos; conclusão nova requer todas as páginas únicas registradas; o indicador de conclusão antigo é preservado; livros fictícios foram retirados dos fluxos de Explorar e busca; o tema claro usa off-white; o contraste secundário claro foi ajustado para passar nos testes; arraste/pinça no modo de página única não muda páginas.

## Catálogo editorial

`app/src/main/assets/lottan-catalog.json` contém catálogo vazio real, URL ainda não configurada e links comerciais desativados. O modelo e as telas aceitam metadados oficiais, destaque, busca, gênero, capas locais e detalhes editoriais. Cada publicação usa identificador estável, título, autor, sinopse, gênero, páginas, idioma, edição, publicação, ISBN, informações adicionais, preço textual oficial, disponibilidade, URL específica e caminho da capa.

Capas devem ser fornecidas em `assets/lottan/covers/`. A URL comercial só é aceita em HTTPS no mesmo host oficial configurado e com a chave comercial explicitamente habilitada para o canal apropriado. Não há pagamento, licença fictícia, autenticação ou confirmação de compra ao voltar do navegador. Arquivos recebidos por Abrir com/Compartilhar são validados como PDFs e importados como pessoais; não recebem selo editorial sem validação de origem editorial.

## Compilação local

A compilação foi realizada localmente com SDK/JDK e dependências já presentes. Não foi usado GitHub, publicação, pipeline remoto ou token. A versão é de teste, com assinatura debug. O ZIP de fontes não inclui chaves privadas, caches, SDK ou JDK. Em outro ambiente, usar Android Studio/JDK 17 e SDK 35 e executar `./gradlew testDebugUnitTest assembleDebug` dentro de `android/`.

O relatório de entrega separado descreve os resultados finais de testes e as limitações de validação.
