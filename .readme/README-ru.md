<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="readium-epub-reader-ic-launcher" border="0" width="128" />
  </p>

  <p>Чтение электронных книг EPUB с оглавлением, поиском, озвучиванием и доступом из скриптов</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Языки (Languages)

******

Текущий README.md поддерживает следующие языки:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-ko.md)
- Русский [ru] # текущий
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-ar.md)

******

### Введение

******

Чтение в одно касание: откройте файл `.epub` прямо из файлового менеджера AutoJs6 основной кнопкой `Читать EPUB` или через контекстное меню. Читалка построена на [Readium Kotlin Toolkit](https://github.com/readium/kotlin-toolkit) 3.4.0, открытом движке, который используют многие коммерческие читалки.

Плагин читает книгу напрямую через временный файловый дескриптор, выданный хостом. Он никогда не получает путь в файловой системе, никуда не копирует книгу и не распаковывает ее в хранилище.

> Текущий этап (сборка разработки 1.0.0): читалка открывает книгу с настройками Readium по умолчанию и показывает оглавление. Запоминание позиции, закладки, настройки, полнотекстовый поиск, озвучивание, фиксированная верстка, импорт шрифтов, отдельная точка входа с лаунчера и скриптовый API `epub` запланированы в ROADMAP.md и пока недоступны.

******

### Основные возможности

******

- Движок Readium: книги EPUB 2 (NCX) и EPUB 3 (NAV) отображаются навигатором Readium с Readium CSS, включая внутренние ссылки, сноски и изображения.
- Без копий: контейнер EPUB читается на месте через дескриптор только для чтения с позиционными чтениями, поэтому даже большие книги открываются без файла кэша.
- Оглавление: переход к любой главе с панели инструментов; вложенные записи сохраняют уровень.
- Внешние ссылки: при нажатии на ссылку `http` или `https` показывается полный адрес, и системный браузер открывается только после подтверждения.
- Интеграция с хостом: меню и диалоги следуют языку и темной теме AutoJs6; конверт Explorer Action строго проверяется до открытия содержимого.
- Многоязычность: интерфейс, инструкции, README и changelog доступны на 10 языках.

******

### Как пользоваться

******

1. Скачайте свежий APK плагина со страницы [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases) и установите его на устройство.
2. Откройте центр плагинов AutoJs6 и включите плагин `Readium EPUB Reader`.
3. В файловом менеджере AutoJs6 нажмите на файл `.epub` или откройте его меню (другие действия) и выберите `Читать EPUB`.
4. Кнопка оглавления на панели инструментов переключает главы; клавиша Назад закрывает читалку.

> Если плагин не появился в центре плагинов, сначала обновите AutoJs6 до свежей версии (внутренняя сборка 5269 или новее). Explorer Action v2 поддерживает основную кнопку и контекстное меню одного файла с временным доступом на чтение документа и родительского каталога.

******

### Поддерживаемые форматы

******

Плагин распознает следующее расширение, а также файлы без расширения, явно отмеченные хостом как `application/epub+zip`:

```text
epub
```

Поддерживается только EPUB: книги с плавающей и фиксированной версткой в EPUB 2 или EPUB 3. Архивы комиксов (CBZ), аудиокниги, PDF и книги с защитой LCP вне области поддержки; книга, помеченная как зашифрованная LCP, сообщается как нечитаемая вместо отображения мусора.

******

### Частые вопросы

******

#### Почему позиция чтения пока не запоминается?

Запоминание позиции и закладки относятся к следующему этапу ROADMAP.md. Текущая сборка всегда открывает книгу с начала.

#### Можно ли менять шрифт, размер текста или тему?

Пока нет. Настройки чтения (шрифт, размер, межстрочный интервал, поля, темы, постраничный или прокручиваемый режим) появятся на этапе настроек; текущая сборка использует значения Readium по умолчанию.

#### Загружает ли плагин мои книги куда-либо?

Нет. У плагина нет собственного сервера. Сеть используется только когда сама книга ссылается на удаленные ресурсы, а также для ручной проверки обновлений, запланированной на отдельной странице настроек.

******

### Разрешения и безопасность

******

Плагин сохраняет поведение Readium по умолчанию для содержимого книги: скрипты и удаленные ресурсы внутри книги не удаляются и не блокируются, включая ресурсы по незащищенному `http://`. Открывайте только книги, которым доверяете.

- Минимум привилегий: плагин получает только временное разрешение на чтение content URI от хоста, никогда не видит путей файловой системы и не записывает книгу в хранилище.
- Строгий конверт: запрос Explorer Action должен содержать ровно одну цель EPUB, ее родительский каталог, подходящую версию протокола, поддерживаемую сборку хоста и оба разрешения на чтение; все остальное отклоняется до открытия файла.
- Ограниченный разбор: поврежденный контейнер (не ZIP, нет `container.xml`, нет документа пакета, выход за пределы контейнера в manifest) завершается сообщением об ошибке, а не сбоем.
- Внешние ссылки показываются полностью и открываются в системном браузере только после подтверждения; схемы кроме `http` и `https` отклоняются.

Манифест запрашивает только сетевое разрешение и разрешение плагина AutoJs6. AndroidX также добавляет ограниченное пакетом signature-разрешение для защиты неэкспортируемых динамических приемников; оно не дает доступа к данным устройства. Разрешения на хранилище, медиа, камеру, геолокацию, специальные возможности или наложения не запрашиваются.

******

### Интерфейс плагина

******

Следующая информация предназначена для разработчиков; хост обнаруживает и запускает плагин по этим идентификаторам:

```text
application id: io.github.supermonster003.autojs6.plugin.readium.epub.reader
service action: org.autojs.plugin.EXPLORER_ACTION
execute action: org.autojs.plugin.EXPLORER_ACTION_EXECUTE
plugin id: readium-epub-reader
engine: explorer-action
variant: default
protocol version: 2
minimum host build: 5269
audited host build: 5282
audited host protocol: 22
```

Explorer Action v2 поддерживает основную кнопку и контекстное меню одного файла с временным доступом на чтение документа и родительского каталога. Требуется AutoJs6 build 5269 или новее.

- [Открыть матрицу совместимости Explorer Action](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/explorer-action-compatibility.md)

******

### Дорожная карта

******

Запланированные возможности и их статус ведутся в ROADMAP.md в виде списка с отметками, разбитого по этапам с критериями приемки: запоминание позиции и закладки, настройки и импорт шрифтов, полнотекстовый поиск, озвучивание, фиксированная верстка, отдельная точка входа приложения, контракт хоста и скриптовый API `epub`. Неотмеченные пункты описывают планы, а не выпущенные возможности. Обратная связь через Issues приветствуется.

- [Открыть ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/ROADMAP.md)

******

### История выпусков

******

#### v1.0.0

_2026/09/19_

- `Подсказка` Сборка для разработки: этапы дорожной карты P0 (каркас, проверка Readium, тестовые образцы) в работе; первый публичный выпуск выходит на этапе P8 дорожной карты
- `Функция` Основная кнопка `Читать EPUB` и действие в контекстном меню для файлов `.epub` в файловом менеджере AutoJs6 (ID плагина `readium-epub-reader`, Explorer Action v2)
- `Функция` База читалки: книги EPUB 2 и EPUB 3 отображаются навигатором Readium, с оглавлением и подтверждаемыми внешними ссылками
- `Функция` Книги читаются на месте через выданный файловый дескриптор с позиционными чтениями; ничего не копируется и не распаковывается в хранилище
- `Функция` Интерфейс, инструкции, README и changelog на 10 языках
- `Зависимость` Добавлен Readium Kotlin Toolkit 3.4.0 (`readium-shared`, `readium-streamer`, `readium-navigator`, `readium-navigator-media-tts`)

##### Дополнительная история выпусков

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/app/src/main/assets/doc/CHANGELOG-ru.md)

******

### Сборка

******

```powershell
.\gradlew.bat :app:assembleDebug
```

Сборка Release:

```powershell
.\gradlew.bat :app:assembleRelease
```

Параметры сборки задаются в `version.properties`. Текущий минимальный SDK равен 24, целевой SDK равен 37.

******

### Локализация и генерация документов

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/values-*/strings.xml
app/src/main/res/raw-*/plugin_instruction.md
```

`strings.xml` локализует сведения о плагине и интерфейс читалки, а `plugin_instruction.md` содержит инструкции, отображаемые хостом. README и changelog всегда правятся через JSON-источники в `.readme/` и `.changelog/` с последующим запуском `py .python/generate_markdown.py`; сгенерированные файлы вручную не редактируются. Команда `py .python/generate_markdown.py --check` проверяет синхронность источников и сгенерированных файлов.

******

### Ссылки

******

- Документация AutoJs6: https://docs.autojs6.com
- Спецификация EPUB 3.3: https://www.w3.org/TR/epub-33/
- Readium Kotlin Toolkit: https://github.com/readium/kotlin-toolkit


[16 KB page alignment and build verification](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/16kb.md)
