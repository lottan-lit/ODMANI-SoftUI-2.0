# ODMANI — Soft UI 2.0

**AUDMANI — Criadora e desenvolvedora da ODMANI.**  
**Parceira editorial — Lottan.**

Desenvolvimento privado e independente do leitor Android nativo. A versão anterior e seus repositórios permanecem preservados.

## Projeto

- `android/`: aplicativo Kotlin/Jetpack Compose, Room, DataStore, PdfRenderer, Navigation e Coroutines.
- `web/`: landing page HTML/CSS/JavaScript, temas claro/escuro/sistema, pronta para hospedagem estática após autorização.
- `docs/`: alterações, validação e distribuição.

Versão de teste: **2.1.0-alpha01**, código Android **4**, Android 8+. O identificador `br.com.lottan.reader.v2` e nomes internos do armazenamento são mantidos para compatibilidade com a versão nativa instalada. A marca exibida é ODMANI. Nomes internos legados não indicam propriedade editorial.

## Atualização

Home modular, retomada compacta, PDFs pessoais separados da biblioteca editorial verificada, detalhes para documentos reais, estatísticas semanais e mapa de atividade, capas originais com cache, superfícies Soft UI e ícone oficial com símbolo 12% menor. A navegação inferior mantém seu código e geometria originais.

Importações sempre entram como pessoais. Não há catálogo oficial integrado nem atribuição baseada no título do arquivo. Os exemplos continuam acessíveis em Explorar e identificados como demonstração; não preenchem a Home ou a biblioteca real.

## Compilar

JDK 17, Android SDK Platform 35 e Build Tools 35.0.0. Em `android/local.properties`, configure seu `sdk.dir`. Execute em `android/`:

```sh
./gradlew assembleDebug testDebugUnitTest assembleDebugAndroidTest lintDebug
```

Para testes em dispositivo: `./gradlew connectedDebugAndroidTest`. O APK de teste é produzido em `android/app/build/outputs/apk/debug/app-debug.apk`. Não distribuir publicamente builds debug. A chave de desenvolvimento deve ser preservada fora do Git para permitir atualizações; uma distribuição de produção exige assinatura apropriada e aprovação.

## Página local

```sh
python3 -m http.server 4182 --directory web
```

Abra `http://localhost:4182`. Não há compilação web nem dependências externas. Sem APK aprovado, o download permanece desabilitado. Capturas reais desta versão aguardam validação em Android; imagens conceituais não são tratadas como screenshots.

## Dados e distribuição

Room migra do esquema 1 para o 2 adicionando apenas a origem com padrão pessoal; nenhuma migração destrutiva. PDFs, progresso, categorias, pastas, conclusão e dias de atividade são preservados. Fontes, zoom e cache não alteram os PDFs originais.

Nenhum segredo, chave de assinatura, documento pessoal, cache ou APK é versionado. O repositório permanece privado. A página e o APK não foram publicados automaticamente. Consulte `docs/PUBLICACAO.md` e `docs/VALIDACAO-SOFTUI2.md`.
