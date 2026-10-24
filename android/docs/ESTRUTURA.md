# Estrutura do projeto

```text
.gitignore
README.md
app/build.gradle.kts
app/proguard-rules.pro
app/schemas/br.com.lottan.nativeapp.data.LottanDatabase/1.json
app/src/androidTest/java/br/com/lottan/nativeapp/DatabaseTest.kt
app/src/androidTest/java/br/com/lottan/nativeapp/NavigationTest.kt
app/src/androidTest/java/br/com/lottan/nativeapp/PdfSessionTest.kt
app/src/main/AndroidManifest.xml
app/src/main/java/br/com/lottan/nativeapp/LottanApplication.kt
app/src/main/java/br/com/lottan/nativeapp/MainActivity.kt
app/src/main/java/br/com/lottan/nativeapp/data/Database.kt
app/src/main/java/br/com/lottan/nativeapp/data/LibraryRepository.kt
app/src/main/java/br/com/lottan/nativeapp/data/PdfSession.kt
app/src/main/java/br/com/lottan/nativeapp/data/Preferences.kt
app/src/main/java/br/com/lottan/nativeapp/domain/ReadingRules.kt
app/src/main/java/br/com/lottan/nativeapp/ui/Demonstration.kt
app/src/main/java/br/com/lottan/nativeapp/ui/Design.kt
app/src/main/java/br/com/lottan/nativeapp/ui/LibraryViewModel.kt
app/src/main/java/br/com/lottan/nativeapp/ui/LottanApp.kt
app/src/main/java/br/com/lottan/nativeapp/ui/Organization.kt
app/src/main/java/br/com/lottan/nativeapp/ui/ReaderScreen.kt
app/src/main/java/br/com/lottan/nativeapp/ui/ReaderViewModel.kt
app/src/main/java/br/com/lottan/nativeapp/ui/SettingsScreens.kt
app/src/main/res/drawable/icon.xml
app/src/main/res/values/styles.xml
app/src/main/res/xml/data_extraction_rules.xml
app/src/test/java/br/com/lottan/nativeapp/ReadingRulesTest.kt
build.gradle.kts
docs/ARQUITETURA.md
docs/AUDITORIA.md
docs/ESTRUTURA.md
docs/REFERENCIA-VISUAL.txt
docs/VALIDACAO.md
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
gradle.properties
gradlew
gradlew.bat
settings.gradle.kts
```

Pacotes separados: data, domain, ui. SDK, caches, chaves e builds intermediários ficam fora do ZIP.
