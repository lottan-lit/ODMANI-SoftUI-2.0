# ODMANI 2.1.0-alpha02 — entrega

APK Android de teste compilado localmente. Android 8 ou posterior. Identificador preservado: br.com.lottan.reader.v2. versionCode 5. Nenhum push, upload, release ou pipeline remoto foi usado.

## Alterações

- Navegação inferior original preservada integralmente.
- Preto absoluto padrão, grafite, claro off-white e automático; contraste elevado e tamanho da interface persistentes.
- Home com leitura principal, fixados, metas configuráveis e documentos reais.
- Biblioteca com busca, ordenação persistente, grade, seleção múltipla e detecção de PDFs idênticos com confirmação.
- Pastas com capas da galeria/arquivos, prévia e ajuste proporcional; um PDF pode pertencer a várias pastas sem cópias físicas adicionais. Exclusão da pasta preserva PDFs.
- Detalhes compactos, renomeação, anotações com salvamento automático e checklist com edição, conclusão, exclusão, reorganização e página opcional vinculada.
- Leitor com toque para alternar controles, página única e rolagem contínua, setas, salto de página, pinça, zoom, ajuste de página/largura, marcadores, fundos e cor personalizada, filtros não destrutivos, brilho, tela ativa, orientação e modo imersivo. Preferências globais e por PDF.
- Abertura de PDFs por Abrir com/Compartilhar do Android, com validação e cópia privada.
- Estatísticas com páginas/tempo, escala explícita, metas, calendário e médias por período. Visitas e páginas únicas são separadas. Novas sessões detalhadas permitem calcular páginas por sessão; o histórico antigo não é artificialmente reconstruído.
- Menu sem hambúrguer redundante; controles de privacidade e limpeza segura do cache de miniaturas.
- Catálogo editorial configurável, vazio nesta entrega. Telas de catálogo e detalhes aceitam metadados reais. Links comerciais permanecem desativados, sem URL oficial fornecida e sem pagamento ou licença simulados.
- Migrações Room 2→3→4 preservam dados e associações anteriores; migração 1→2 original mantida.

## Bugs corrigidos

BUG-001: escala do gráfico exagerava pequenas durações. Corrigida com escala mínima de 30 minutos ou 30 visitas, unidades e dias zerados. Importação não gera leitura. Visita exige permanência de cinco segundos. Tempo interrompe no segundo plano e após dois minutos sem interação.

BUG-002: removido o hambúrguer do Menu e configurações, mantendo voltar e navegação sem empilhar repetidamente o mesmo destino.

Também corrigidos sobreposição na Home estreita, progresso de PDFs nunca abertos, conclusão prematura de PDFs de uma página, contraste insuficiente no tema claro e conteúdo fictício nos fluxos de Explorar/busca.

## Verificações

- Compilação final: BUILD SUCCESSFUL, com testDebugUnitTest, assembleDebug e assembleDebugAndroidTest.
- 16 testes unitários passaram.
- A rodada Android anterior aos últimos ajustes passou nos 11 testes instrumentados: importação/duplicados, várias pastas, banco, migração, navegação principal/Menu, PDF, toque para alternar controles, miniaturas e persistência de notas/checklist/metas/preferências.
- Os últimos ajustes de navegação individual, pinça contínua e referências de checklist foram compilados; não foi repetida a rodada de emulador, conforme solicitado.
- Inspeção visual realizada na Home clara/escura e no Menu. Sem validação em aparelho físico ou tablet.

## Limitações

Backup completo e restauração não estão implementados; a interface informa essa limitação. Publicações oficiais, capas e URLs comerciais precisam ser fornecidas pela editora no arquivo de catálogo. Compra, autorização editorial e entrega de PDFs pelo site dependem da infraestrutura externa; arquivos compartilhados são importados como pessoais, sem inventar validação editorial. O estado de catálogo preenchido não foi validado com publicações oficiais reais, pois nenhuma foi fornecida.

Esta é uma versão alpha de teste; os testes descritos não equivalem à validação completa em todos os aparelhos e fluxos.

## Instalação por USB

1. Copie o APK para Downloads no smartphone por cabo USB.
2. Abra o arquivo no celular e permita a instalação por essa origem, se solicitado.
3. Confirme Instalar.

O APK usa assinatura de teste. Atualização sobre a instalação existente exige assinatura compatível; a assinatura do seu celular não foi verificada. Se o Android recusar a atualização por assinatura, não desinstale a versão anterior se precisar preservar seus dados.

## Código-fonte

O ZIP inclui o projeto atualizado, schemas e testes, sem SDK, caches, chaves privadas ou diretórios de compilação. O diagnóstico técnico está em android/docs/DIAGNOSTICO-ALPHA02.md. Documentos antigos do repositório descrevem versões anteriores; este relatório descreve esta entrega.
