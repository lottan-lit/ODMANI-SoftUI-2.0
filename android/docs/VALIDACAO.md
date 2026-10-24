# Validação e limites desta alpha

## Evidências obtidas neste ambiente

- Projeto Gradle reconhecido com JDK17/Gradle8.9/AGP8.7.3.
- APK debug compilado com interface Kotlin/Compose e assinado com certificado de desenvolvimento.
- Seis testes JVM executados, sem falhas: limites de página; normalização de título; limiares de intensidade; divisão à meia-noite; dia com horário de verão; intervalos inválidos sem atividade inventada.
- Testes instrumentados de banco, navegação e PdfRenderer compilados em APK separado; **não executados** por ausência de aparelho/emulador Android neste ambiente.
- Análise estática Android Lint executada: sem erros. Avisos de versões mais novas das bibliotecas foram mantidos intencionalmente; versões fixadas constam da arquitetura.
- Manifesto inspecionado: sem INTERNET e sem acesso amplo ao armazenamento; apenas permissão interna gerada pelo AndroidX. Assinatura APK verificada com apksigner.

## Comparação com o MVP e pendências explícitas

A estrutura de rotas, textos principais, fontes de dados, temas neutros, ícones desenhados em Canvas e controles foram convertidos com base na referência CSS/Java auditada. Isso é evidência de conversão de código, **não de equivalência visual100%**.

Não foram feitas capturas da execução nativa. Permanecem obrigatórios: comparação lado a lado em claro/escuro; correção das diferenças de dimensões/tipografia; validação dos reflexos/vidro (aproximação, sem backdrop blur idêntico); toque/gestos; TalkBack e fonte ampliada; retrato/paisagem; Moto G84 e tablet; desempenho com biblioteca grande; PDFs diversos e protegidos; processo morto durante importação/leitura.

Os estados de erro e caminhos de arquivo foram implementados, mas testes físicos de SAF/importação, confirmação de exclusão e liberação de memória do PdfRenderer não foram executados. O teste de renderização compilado exercita limite de pixels, cache e arquivo inválido quando executado no Android.

Pastas/categorias são virtuais e podem ser excluídas preservando PDFs. O progresso e o tempo desta instalação começam localmente. A migração do localStorage antigo e de dados entre os pacotes **não está implementada** nesta alpha de comparação. O MVP não foi substituído.

Esta entrega é uma primeira alpha nativa instalável para testes; a migração com fidelidade visual integral não está declarada concluída. O SDK também sinaliza ausência de platform-tools/licença neste ambiente: isso não impediu assemble/lint/testes JVM, mas adb/emulador não foram usados. Configure e aceite as licenças no seu ambiente conforme README.

## Instalador entregue

Arquivo: LOTTAN-v2-2.0.0-alpha01.apk

Bytes: 11468542

SHA-256: `7d1a74579690a86ae74496e21b10ef0c5d0d8889246a1c1ef4fbd839a7b75337`

Verificação final Gradle: assembleDebug, testDebugUnitTest e lintDebug concluídos com sucesso. APK instrumentado também compilado; não executado.
