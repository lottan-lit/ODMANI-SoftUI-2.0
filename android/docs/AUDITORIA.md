# Auditoria do MVP 0.2.5 e contrato de migração

Fonte integral analisada: assets/index.html, MainActivity.java, PdfActivity.java, SystemBars.java, manifest, recursos e build.py do MVP. A referência CSS integral está em REFERENCIA-VISUAL.txt. Nada desse CSS é executado pelo novo app.

## Inventário de telas e fluxos
| Tela | Elementos e comportamento a preservar |
|---|---|
| Entrada | lottan, Leitura em outro ritmo., texto editorial, seta circular e pontos; entrada persistente |
| Início vazio | marca, busca/menu; Sua próxima leitura começa aqui.; importar primeiro PDF; prateleira de demonstração |
| Início com PDF | Continuar lendo, capa 120×183 aproximadamente, título, progresso, Continuar; Minha biblioteca, seta; prateleira horizontal; importar |
| Biblioteca | cabeçalho, Pastas e categorias, contagem, importar; documentos com metadados, exclusão confirmada; demonstração |
| Explorar | Livros/Revistas/Jornais, banner editorial, capas de demonstração, busca |
| Busca | busca por título, resultados locais e demonstração, estado vazio |
| Atividade | tempo total, visitas, importados; mapa de intensidade Semana/Mês/Ano, seleção diária; nada de atividade inventada |
| Organização | Pastas/Categorias; formulário; Todos; cartões com contagem; seleção de pasta/categoria por PDF; excluir grupo preservando documentos |
| Menu | tema Claro/Escuro/Sistema; Páginas do PDF; Conforto de leitura; documentos/importar/atividade; ajuda/sobre |
| Páginas do PDF | ajuste, zoom, rotação, direção; opções de diagramação explicadas como limitadas em PDFs fixos |
| Conforto | preferências de texto demonstrativo; controles; limite das configurações explicado |
| Detalhe demonstração | capa, título, autor, páginas/gênero/formato, continuar, favorito/informações |
| Leitura demonstração | capítulo e prosa serifada; tamanho/espaçamento/tema; posição e controles |
| Importação | escolher no seletor, PDF sem senha até 200 MB, cópia privada, original preservado |
| Leitor real | página única, PdfRenderer, anterior/próximo/seek/salto, pinça/pan/dois toques, rotação/ajuste, progresso persistente |
| Ajuda/Sobre | textos honestos sobre recursos e limitações |

## Tokens e navegação
Tons de cinza confirmados pelo usuário (prevalecem sobre a menção colorida no pedido). Fundo claro #ededed, escuro #191919; manchas radiais e anel difuso grandes; vidro translúcido com borda/reflexo. Fonte de interface Arial/sans-serif no MVP; prosa Georgia/serif. Marca ~33sp, cabeçalhos 18sp, corpo ~13sp. Margens móveis 24dp. Cápsula inferior 53dp, botões 46×43dp, ícones fixos22dp; ativo com rótulo12sp; biblioteca separada53dp. Barra visível apenas nas telas principais, sem miniaturizar ícones ativos. Insets dinâmicos do sistema e teclado, cores integradas, botões Android visíveis. Transições curtas, desativadas com escala de animação zero.

## Dados existentes e riscos
PDFs: files/pdfs/UUID.pdf; metadados SharedPreferences library/documents; progresso reading/UUID; totais reading/seconds e visited; diário reading/days. Pastas/categorias e temas estão em localStorage da WebView. O novo app não acessa DOM nem executa JS: importar automaticamente esse localStorage privado exige exportação intermediária pelo MVP. Não apagar a instalação antiga. A versão v2 de teste usa applicationId separado .v2 para comparação lado a lado; isso implica reimportar os PDFs no teste. A promoção para atualização do pacote original exige ponte de exportação e testes de migração antes de publicar substituição.

## Diferenças técnicas indispensáveis (declaradas antes da implementação)
Arial/Georgia não estão distribuídas no MVP e não podem ser empacotadas sem licença; usar sans-serif/serif do Android, com métricas próximas e verificação pendente. CSS backdrop-filter não existe como equivalente universal no Compose/API26: reproduzir superfícies com transparência, gradientes e reflexos nativos; blur do fundo em API31+ quando suportado, sem prometer equivalência pixel a pixel. Material3 completamente personalizado, sem navegação genérica. Não alegar fidelidade total sem capturas comparáveis no aparelho/emulador.
