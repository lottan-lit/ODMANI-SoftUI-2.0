# ODMANI Soft UI 2.0

Leitor Android desenvolvido pela AUDMANI. Parceira editorial: Lottan.

## Baixar o APK de teste

**[Abrir o site e baixar no celular](https://lottan-lit.github.io/ODMANI-SoftUI-2.0/#download)**

APK **2.2.0-softui01**, código **5**, Android 8 ou posterior, **11,24 MB**. Este é um build Debug para testes, validado no Android 15. Versão, tamanho e SHA-256 estão em `web/assets/release.json`.

## Organização e versões

- `web/`: site público, capturas reais e APK aprovado para download.
- `android/`: fontes Android da versão de origem, **2.1.0-alpha01 / código 4**. Não correspondem ao APK 2.2.0 publicado.
- `docs/`: documentação; as notas antigas são registros da versão de origem.
- `.github/workflows/pages.yml`: publica exclusivamente o conteúdo de `web/`.
- O fluxo antigo de importar fontes é legado; não é necessário para publicar o site.

O proprietário autorizou publicar somente o site e o APK. As fontes atualizadas da versão 2.2.0 e os registros detalhados de validação permanecem locais. Não foram enviados chaves, credenciais, PDFs pessoais ou configurações privadas.

O pacote ZIP antigo com fontes duplicadas foi retirado da raiz; o histórico permanece recuperável no Git. Para compilar as fontes da versão de origem, consulte `android/README.md`.

## Instalação e atualização

Abra o site no celular, toque em Baixar APK de teste e siga as instruções do Android. Atualizar uma instalação existente exige certificado compatível. Evite desinstalar para preservar documentos e histórico.

13 testes unitários e 6 instrumentados passaram no APK publicado. A validação incluiu importação pelo seletor Android, renderização, capas, zoom e retomada após reiniciar o processo. Limite: emulador Android 15; sem teste em aparelho físico ou outras APIs.
