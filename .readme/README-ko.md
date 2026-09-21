<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="readium-epub-reader-ic-launcher" border="0" width="128" />
  </p>

  <p>EPUB 전자책을 읽고 목차, 검색, 읽어주기와 스크립트 추출을 제공</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 언어 (Languages)

******

현재 README.md는 다음 언어를 지원합니다:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-ja.md)
- 한국어 [ko] # 현재
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-ar.md)

******

### 소개

******

한 번의 탭으로 읽기: AutoJs6 파일 관리자에서 `.epub` 파일을 바로 엽니다. 기본 버튼 `EPUB 읽기`로도, 메뉴에서도 열 수 있습니다. 리더는 많은 상용 리더가 사용하는 오픈 소스 엔진 [Readium Kotlin Toolkit](https://github.com/readium/kotlin-toolkit) 3.4.0 위에 만들어졌습니다.

플러그인은 호스트가 부여한 임시 파일 디스크립터로 책을 직접 읽습니다. 파일 시스템 경로를 받지 않고, 책을 어디에도 복사하지 않으며, 저장소에 압축을 풀지도 않습니다.

> 1.1.0이 현재 릴리스이고, 1.0.0이 첫 릴리스입니다. 리더는 EPUB 2와 EPUB 3 책을 목차와 함께 열고, 책마다 읽던 위치를 기억하며, 스크롤 모드, 탭 영역, 볼륨 키, 몰입 모드, 환경설정 패널 (글자 크기, 글꼴, 간격, 정렬, 단, 호스트의 야간 모드를 따를 수 있는 테마), 가져온 TTF / OTF 글꼴, CJK 세로쓰기와 오른쪽에서 왼쪽 책, 단일 페이지 또는 펼침면으로 보는 고정 레이아웃 책, 전체 텍스트 검색, 북마크, 책 안의 링크, 주석과 이미지, 그리고 시스템 텍스트 음성 변환 엔진을 쓰는 소리 내어 읽기를 제공합니다. 앱 아이콘은 최근 책과 시스템 문서 선택기가 있는 런처를 열고, 다른 앱은 `ACTION_VIEW`로 EPUB을 넘길 수 있으며, 설정 페이지는 리더 기본값, 기기에 보관되는 데이터, 수동 업데이트 확인을 다룹니다. `epub` 스크립트 API, 호스트 리더 세션, 샘플 스크립트 세 개는 AutoJs6 6.8.0 (빌드 5282)과 함께 제공됩니다. 1.1.0은 하이라이트와 메모를 추가합니다 (ROADMAP.md, P9): 선택한 텍스트를 네 가지 색으로 하이라이트하거나 밑줄을 긋고 메모를 붙일 수 있으며, 하이라이트는 페이지에 그려지고 패널에 나열됩니다 (이동, 편집, 삭제). 책의 하이라이트와 메모는 시스템 공유로 Markdown 으로 내보내거나 파일로 저장할 수 있습니다. EPUB 계약 버전 2를 지닌 호스트 (5282보다 새로운 AutoJs6 빌드)는 `book.annotations()`로 이를 읽고 리더 세션에서 `highlight` 이벤트를 받으며, AutoJs6 6.8.0 (빌드 5282)은 계약 버전 1로 계속 동작합니다.

******

### 스크린샷

******

`docs/fixtures`로 생성한 샘플 책을 휴대폰에서 찍은 것입니다 (제3자의 책은 보여주지 않습니다). 인터페이스 언어는 AutoJs6의 언어를 따르며 여기서는 영어입니다:

<table>
  <tr>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/reader.png?raw=true" alt="reader" width="180" /><br/>읽기</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/table-of-contents.png?raw=true" alt="table-of-contents" width="180" /><br/>목차</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/preferences.png?raw=true" alt="preferences" width="180" /><br/>읽기 환경설정</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/search.png?raw=true" alt="search" width="180" /><br/>전체 텍스트 검색</td>
  </tr>
  <tr>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/bookmarks.png?raw=true" alt="bookmarks" width="180" /><br/>북마크</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/read-aloud.png?raw=true" alt="read-aloud" width="180" /><br/>소리 내어 읽기</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/dark-theme.png?raw=true" alt="dark-theme" width="180" /><br/>어두운 테마</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/sepia-theme.png?raw=true" alt="sepia-theme" width="180" /><br/>세피아 테마</td>
  </tr>
  <tr>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/vertical-ja.png?raw=true" alt="vertical-ja" width="180" /><br/>일본어 세로쓰기</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/fixed-layout.png?raw=true" alt="fixed-layout" width="180" /><br/>고정 레이아웃</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/launcher.png?raw=true" alt="launcher" width="180" /><br/>최근 책</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/settings.png?raw=true" alt="settings" width="180" /><br/>설정</td>
  </tr>
</table>

******

### 주요 기능

******

- Readium 엔진: EPUB 2 (NCX)와 EPUB 3 (NAV) 책을 Readium 내비게이터와 Readium CSS로 렌더링하며 내부 링크, 각주, 이미지를 지원합니다.
- 복사 없음: EPUB 컨테이너를 읽기 전용 디스크립터에서 위치 기반으로 읽으므로 큰 책도 캐시 파일 없이 열립니다.
- 목차: 도구 모음에서 원하는 장으로 이동하며 중첩 항목은 깊이를 유지합니다.
- 읽기 위치 기억: 각 책의 마지막 위치를 내용 지문으로 플러그인 전용 저장소에 보관하므로 책을 옮기거나 이름을 바꿔도 이어서 읽을 수 있습니다; `처음부터 읽기`로 지울 수 있습니다.
- 리더 화면: 툴바에 제목과 챕터, 위치와 백분율을 보여 주는 진행률 표시줄, 가운데 탭으로 몰입 모드, 탭 영역과 볼륨 키로 페이지 넘기기, 스크롤 또는 페이지 모드.
- 읽기 설정: 하단 패널에서 글자 크기, 글꼴, 줄 간격, 페이지 여백, 문단 간격, 정렬, 하이픈 연결, 출판사 스타일, 단 수, 페이지 또는 스크롤 레이아웃을 설정합니다; 변경은 즉시 적용되고 모든 책에서 기억됩니다. 라이트, 세피아, 다크 테마 또는 호스트의 야간 모드 따르기; 툴바와 시스템 바는 테마 색상을 사용합니다.
- 글꼴 가져오기: 시스템 문서 선택기로 TTF 또는 OTF 파일을 고르면 검증을 거쳐 플러그인 안에 비공개로 저장되고 (최대 10개, 파일당 20 MB), 읽기 설정 패널의 기본 글꼴 뒤에 나열되며, 모든 책에 적용되고, 같은 패널에서 삭제할 수 있습니다.
- CJK 세로쓰기와 오른쪽에서 왼쪽으로 읽는 책: 읽기 진행 방향은 출판물을 따르므로 오른쪽에서 왼쪽 책에서는 탭 영역이 반전됩니다. 페이지 진행이 오른쪽에서 왼쪽인 일본어와 중국어 책은 세로쓰기로 표시되며, `글자 방향` 설정으로 가로쓰기 또는 세로쓰기를 강제할 수 있습니다. 인터페이스 자체의 레이아웃 방향은 AutoJs6 언어를 따르며 책과 독립적입니다.
- 고정 레이아웃 책: 페이지 번호는 `x / N 페이지`로 표시되고, 패널은 `펼침 보기` 선택 (자동은 가로 방향에서 두 페이지를 나란히 표시)을 제공하며 적용되지 않는 글자 설정을 숨깁니다. 손가락 확대와 끌기는 Readium 내장입니다.
- 전체 텍스트 검색: 도구 모음의 `검색` 항목이 책 안의 모든 일치 항목을 50개씩 (최대 500개) 장별로 문맥과 함께 찾아 주고, 결과를 탭하면 해당 위치로 이동하여 페이지의 일치 항목을 강조하며, 진행 표시줄 위에서 이전 / 다음으로 이동합니다.
- 북마크: 도구 모음 아이콘이 현재 페이지를 표시하고 (북마크된 페이지에서는 채워진 아이콘), `북마크` 항목이 모든 북마크를 장 이름, 발췌, 시각과 함께 최신순으로 나열하여 이동, 삭제, 모두 지우기를 제공합니다. 책마다 (최대 500개) 읽기 위치와 함께 저장됩니다.
- 제스처, 키, 링크: 탭으로 페이지 넘기기 (끄기, 좌우, 상하), 볼륨 키, 하드웨어 키보드 키와 선택 텍스트 메뉴 (복사, 공유, 웹 검색, 텍스트 처리 앱); 책 안의 링크는 뒤로 가기 스택을 유지하고, 주석은 대화 상자에 표시되며, 외부 링크는 확인 후 또는 바로 열리고, 탭한 이미지는 전체 화면으로 열립니다.
- 읽어 주기: 더보기 메뉴의 `읽어 주기`는 시스템 텍스트 음성 변환 엔진으로 현재 페이지부터 책을 읽어 주고, 읽고 있는 문장을 강조하며 페이지를 자동으로 넘깁니다. 페이지 아래의 바와 미디어 알림에서 재생 / 일시정지, 이전 / 다음 문장, 중지를 조작할 수 있고, 헤드셋 버튼이 동작하며, 속도, 음높이, 언어, 음성을 조정할 수 있고, 화면이 꺼져도 읽어 주기가 계속되며, 리더를 닫으면 중지됩니다 (`백그라운드에서 계속`을 켠 경우 제외). 읽어 주기 설정에는 수면 타이머 (15 / 30 / 60분 또는 이 장의 끝)와 화면 켜짐 유지 스위치도 있습니다.
- 외부 링크: `http` 또는 `https` 링크를 탭하면 전체 주소를 표시하고 확인 후에만 시스템 브라우저를 엽니다.
- 독립 실행 런처: 앱 아이콘이 표지, 제목, 저자, 진행률, 마지막 읽은 시각이 있는 최근 책 그리드와 시스템 문서 선택기로 책을 고르는 `EPUB 열기` 버튼을 엽니다. 리더는 파일 관리자에서 여는 것과 같습니다.
- 다른 앱에서 열기: 파일 관리자, 브라우저, 메일 앱이 `ACTION_VIEW`로 `content://` EPUB을 넘길 수 있습니다. 오버플로 메뉴의 `최근 책에 추가`는 보낸 앱이 지속적인 접근을 허용할 때 런처에 남깁니다.
- 설정 페이지: 테마, 페이지 넘기기, 소리 내어 읽기 기본값, 링크, 데이터 관리와 함께 릴리스 기록 및 탭할 때만 GitHub에 묻는 수동 업데이트 확인
- 스크립트 서비스: `org.autojs.plugin.EPUB` Binder 서비스로 AutoJs6 호스트는 리더를 열지 않고도 책을 읽을 수 있습니다 (메타데이터, 목차, 읽기 순서, 일반 텍스트 또는 경량 Markdown 장 텍스트, 리소스, 전체 텍스트 검색, 위치 수). 요청에는 상한이 있고, 동시에 최대 8권까지 열 수 있으며, 접근은 호스트로 제한됩니다. AutoJs6 6.8.0은 이를 `epub` 모듈로 스크립트에 노출합니다 (아래 "스크립트에서 사용" 참조).
- 호스트 리더 세션: AutoJs6 호스트는 `org.autojs.plugin.EPUB` 서비스를 통해 책의 리더를 열고 따라갈 수 있으며 (위치, 북마크, 닫힘 이벤트), 로케이터, href 또는 진행률로 이동하고, 페이지나 장을 넘기고, 읽기 설정을 바꿀 수 있습니다. 리더는 호스트가 일회용 세션 토큰을 담아 명시적으로 시작할 때만 열리며, 세션을 닫아도 호스트가 종료를 요청하지 않는 한 리더는 사용자에게 남습니다.
- 호스트 연동: 메뉴와 대화 상자는 AutoJs6의 언어와 다크 모드를 따르며, Explorer Action 봉투는 콘텐츠를 열기 전에 엄격하게 검증됩니다.
- 다국어: 인터페이스, 설명, README, changelog를 10개 언어로 제공합니다.

******

### 설치

******

1. 플러그인 센터에서: AutoJs6에서 `플러그인`을 열고 공식 목록에서 `Readium EPUB Reader`를 골라 설치를 누릅니다. 플러그인 센터가 서명된 APK를 내려받아 설치하고 플러그인을 활성화할 수 있게 합니다.
2. GitHub에서: [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases) 페이지에서 APK를 내려받고 (파일 이름에 CRC32가 붙고 `SHA256SUMS`에 체크섬이 있습니다) 설치한 뒤 플러그인 센터에서 플러그인을 활성화합니다.
3. 요구 사항: 파일 관리자 진입점에는 AutoJs6 내부 빌드 5269 이상, `epub` 스크립트 API에는 AutoJs6 6.8.0 (빌드 5282) 이상, Android 7.0 이상, 그리고 시스템 WebView가 필요합니다.

******

### 사용 방법

******

1. [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases) 페이지에서 최신 플러그인 APK를 내려받아 기기에 설치합니다.
2. AutoJs6의 플러그인 센터를 열고 `Readium EPUB Reader` 플러그인을 활성화합니다.
3. AutoJs6 파일 관리자에서 `.epub` 파일을 탭하거나 메뉴 (더 보기)를 열어 `EPUB 읽기`를 선택합니다.
4. 툴바의 목차 버튼으로 챕터 사이를 이동하고 설정 버튼으로 글자와 테마를 조정하며, 페이지의 왼쪽이나 오른쪽 3분의 1을 탭하거나 볼륨 키를 눌러 페이지를 넘기고, 가운데를 탭해 툴바를 숨기거나 표시합니다; 뒤로 가기를 누르면 리더가 닫히고 위치가 기억됩니다.
5. 파일 관리자를 거치지 않으려면 앱 아이콘을 누르세요. 런처가 최근 책을 나열하고 `EPUB 열기`가 시스템 문서 선택기로 책을 고릅니다. 이렇게 연 책은 표지와 진행률과 함께 목록에 남습니다.
6. 다른 앱 (파일 관리자, 브라우저의 다운로드, 메일 첨부)에서 `.epub` 파일에 이 리더를 선택하세요. 책은 같은 방식으로 열리고, 오버플로 메뉴의 `최근 책에 추가`는 보낸 앱이 지속적인 접근을 허용할 때 런처 목록에 남깁니다.
7. 런처 메뉴나 리더 오버플로 메뉴에서 `설정`을 열어 테마, 페이지 넘기기, 소리 내어 읽기 기본값, 링크를 설정하고, 플러그인이 보관하는 데이터를 지우고, 릴리스 기록을 읽거나 업데이트를 확인할 수 있습니다 (확인은 탭할 때만 GitHub에 접속합니다).
8. 스크립트에서: `epub.open(path)`로 책을 읽고 (메타데이터, 목차, 텍스트, 검색) `epub.read(path)`로 이 리더를 열어 위치를 보고받습니다. 아래 "스크립트에서 사용"과 AutoJs6의 `전자책` 샘플을 참조하세요.

> 플러그인 센터에 플러그인이 보이지 않으면 먼저 AutoJs6를 최신 버전 (내부 빌드 5269 이상)으로 업데이트하세요. Explorer Action v2는 단일 파일의 기본 버튼과 메뉴를 지원하며 문서와 상위 폴더의 임시 읽기 권한을 사용합니다.

******

### 스크립트에서 사용

******

AutoJs6 6.8.0은 전역 모듈 `epub` (별칭 `$epub`)을 추가하며 이 플러그인이 그 요청을 처리합니다: 리더를 열지 않고 책을 읽거나, 스크립트에서 리더를 열고 위치를 따라갈 수 있습니다. AutoJs6에는 `샘플 > 전자책` 아래에 샘플 스크립트 세 개가 들어 있고, 참조 문서는 [AutoJs6 문서](https://docs.autojs6.com/#/epub)에 있습니다:

메타데이터, 목차, 장 텍스트:

```javascript
let book = epub.open('./books/lighthouse.epub');
console.log(book.metadata.title, '-', (book.metadata.authors || []).join(', '));
book.toc.forEach(entry => console.log(entry.title, entry.href, (entry.children || []).length, 'children'));
console.log(book.readingOrder.length, 'resources,', book.positions, 'positions');
let first = book.readingOrder[0];
console.log(book.text(first.href, { format: 'markdown' }));
book.close();
```

표지, 검색, 편의 함수:

```javascript
let path = './books/lighthouse.epub';
let book = epub.open(path);
try {
    console.log('cover saved to', book.cover(files.cwd(), { overwrite: true }));
} catch (e) {
    if (!(e instanceof epub.EpubError) || e.code !== 'RESOURCE_NOT_FOUND') throw e;
    console.log('this book has no cover');
}
book.search('lighthouse', { limit: 20 }).forEach(hit => console.log(hit.title || hit.href, ':', hit.text));
files.write('./lighthouse.txt', book.textAll({ maxChars: 2 * 1024 * 1024 }));
book.close();
console.log(epub.metadata(path).language); // the convenience functions open and close the book themselves
epub.tocAsync(path).then(toc => console.log(toc.length, 'entries'));
```

리더 열기와 위치 따라가기:

```javascript
let session = epub.read('./books/lighthouse.epub', { progression: 0.25, preferences: { theme: 'sepia' } });
session.on('open', e => console.log('opened', e.title, 'at', e.href, '|', e.positions, 'positions'));
session.on('progress', e => console.log((e.totalProgression * 100).toFixed(1) + '%', e.chapterTitle || e.href));
session.on('bookmark', e => console.log('bookmark', e.action, e.locator.href, '| total', session.bookmarks().length));
session.on('close', e => console.log('closed:', e.reason)); // user, host, replaced, timeout, error or overflow
setTimeout(() => session.isOpen && session.nextChapter(), 30 * 1000);
setTimeout(() => session.isOpen && session.close(), 60 * 1000);
```

경로는 스크립트 작업 디렉터리 기준 상대 경로이거나 절대 경로입니다 (`content://` URI는 받지 않습니다). 플러그인이나 책을 사용할 수 없으면 모든 호출이 `code`가 있는 `EpubError`를 던집니다 (`PLUGIN_UNAVAILABLE`, `NOT_EPUB`, `ENCRYPTED`, `PARSE_FAILED`, `TIMEOUT` 등). `epub.isAvailable()`은 플러그인이 설치되고 활성화되었는지 알려주며, 모든 메서드에는 Promise를 반환하는 `*Async` 짝이 있습니다.

******

### 지원 형식

******

플러그인은 다음 확장자를 인식하며 호스트가 `application/epub+zip`으로 명시한 확장자 없는 파일도 허용합니다:

```text
epub
```

EPUB만 지원합니다: EPUB 2 또는 EPUB 3의 리플로우와 고정 레이아웃 책. 만화 아카이브 (CBZ), 오디오북, PDF, LCP로 보호된 책은 범위 밖입니다. LCP 암호화로 표시된 책은 깨진 내용을 표시하지 않고 읽을 수 없다고 알립니다.

******

### 호환성

******

플러그인에 필요한 것, 검증한 환경, 범위 밖인 것:

- AutoJs6: 파일 관리자 진입점 (Explorer Action v2)에는 내부 빌드 5269 이상이 필요합니다. `epub` 스크립트 API, 호스트 리더 세션, 샘플 스크립트에는 AutoJs6 6.8.0 (빌드 5282)이 필요하며, 이는 이번 릴리스에서 감사한 마지막 호스트 빌드입니다.
- Android 7.0 (API 24)부터 Android 16 (API 37, 대상)까지. 페이지는 기기의 WebView에서 렌더링되므로 최신 Android System WebView 또는 Chrome이 필요합니다. 플러그인에는 네이티브 라이브러리가 없어 16 KB 페이지 기기에서도 그대로 실행됩니다.
- 검증 완료: AVD API 24 / 33 / 36 / 37, Sony Xperia XZ1 Compact (Android 9), Redmi 12C (Android 13, MIUI), Xiaomi Pad 6 (Android 15, 서비스 측). 기기 x 시나리오 매트릭스, 편차, WebView 버전은 `docs/dev/compatibility-matrix.md`에 있습니다.
- 책: EPUB 2와 EPUB 3, 리플로우와 고정 레이아웃, CJK 세로쓰기와 오른쪽에서 왼쪽. DRM으로 보호된 책 (LCP, Adobe ADEPT)은 보호됨으로 보고되며 렌더링되지 않습니다. PDF, MOBI, AZW, CBZ, 오디오북은 범위 밖입니다.
- 소리 내어 읽기에는 책의 언어 음성 데이터가 있는 텍스트 음성 변환 엔진이 필요합니다 (Google 음성 서비스, 제조사 엔진, 그 밖의 설치된 엔진). 사용할 수 있는 엔진이 없는 기기는 조용히 있지 않고 약 20초 뒤에 알립니다.
- 크기와 성능: release APK는 약 3.3 MB입니다. 200 MB 책은 2017년 휴대폰에서 1 ~ 3초에 열리고, 위치와 지문 계산은 첫 페이지를 늦추지 않으며, 수천 개 장이 있는 책은 여는 데 눈에 띄게 오래 걸립니다 (`docs/dev/performance-baseline.md`).

******

### 자주 묻는 질문

******

#### 읽기 위치는 어떻게 기억되나요?

각 책의 마지막 위치는 경로가 아니라 파일 내용의 지문을 키로 플러그인 전용 저장소에 저장되므로 같은 책을 다시 열면 읽던 곳에서 이어집니다. 더보기 메뉴에서 `처음부터 읽기`를 선택하면 지워집니다.

#### 글꼴, 글자 크기, 테마를 바꿀 수 있나요?

네. 툴바에서 설정 패널을 열어 글자 크기, 글꼴 (출판사 기본값, 세리프, 산세리프, 고정폭 또는 Readium에 포함된 접근성 글꼴), 줄 간격, 여백, 간격, 정렬, 단 수, 테마 (라이트, 세피아, 다크 또는 호스트 따르기)를 설정할 수 있습니다. 패널의 `글꼴 가져오기`를 누르면 자체 TTF 또는 OTF 파일을 추가할 수 있습니다. 글꼴은 플러그인 안에 비공개로 저장되며 `글꼴 관리`에서 삭제할 수 있습니다.

#### 이 플러그인이 내 책을 어딘가로 업로드하나요?

아니요. 플러그인에는 자체 서버가 없습니다. 네트워크는 책 자체가 원격 리소스를 참조할 때와 설정 페이지의 수동 업데이트 확인 (탭할 때만 HTTPS로 GitHub Releases API에 묻고 아무것도 내려받지 않음)에만 사용됩니다.

#### PDF, MOBI, AZW 파일은 왜 지원하지 않나요?

리더는 Readium 툴킷 위에 만들어졌고 EPUB만 렌더링합니다. PDF에는 다른 렌더러가 필요하고, MOBI / AZW는 공개 렌더링 엔진이 없는 Amazon 형식입니다. 먼저 Calibre 같은 도구로 EPUB으로 변환하세요. 만화 아카이브 (CBZ)와 오디오북도 범위 밖입니다.

#### 소리 내어 읽기에서 소리가 나지 않습니다

플러그인은 시스템 설정에서 고른 텍스트 음성 변환 엔진으로 말합니다 (`접근성 > 텍스트 음성 변환 출력`). 책의 언어 음성 데이터가 있는 엔진이 설치되어 있는지, 미디어 볼륨이 올라가 있는지, 다른 앱이 오디오 포커스를 잡고 있지 않은지 (통화나 음악은 읽기를 일시정지합니다) 확인하세요. 엔진이 말할 수 없는 언어의 책은 엔진의 기본 음성을 쓰고, 사용할 수 있는 엔진이 없는 기기는 약 20초 뒤에 메시지를 보여줍니다.

#### 가져온 글꼴이 책에 적용되지 않습니다

출판사 스타일이 자체 글꼴을 고정했을 수 있습니다: 환경설정 패널에서 `출판사 스타일`을 끄고 가져온 글꼴을 다시 선택하세요. TTF와 OTF 파일만 받으며 (글꼴 모음 `.ttc`는 별도 메시지로 거부됩니다), 글꼴은 본문에 적용되고 출판사가 특정 글꼴 패밀리를 지정한 제목은 그대로 유지됩니다.

#### 일본어나 중국어 세로쓰기 책은 어떻게 처리되나요?

spine이 오른쪽에서 왼쪽 페이지 진행을 선언하고 언어가 일본어나 중국어인 책은 세로로 렌더링되며 오른쪽에서 왼쪽으로 페이지를 넘깁니다. `텍스트 방향` 환경설정으로 어떤 책이든 가로 또는 세로 텍스트를 강제할 수 있습니다. 인터페이스는 AutoJs6 언어의 방향을 유지하므로 영어 인터페이스는 왼쪽에서 오른쪽 그대로이고 책은 오른쪽에서 왼쪽으로 읽습니다.

******

### 권한과 보안

******

플러그인은 책 콘텐츠에 대해 Readium의 기본 동작을 유지합니다. 책 안의 스크립트와 원격 리소스는 제거되거나 차단되지 않으며 평문 `http://` 리소스도 포함됩니다. 신뢰할 수 있는 책만 여세요.

- 최소 권한: 플러그인은 호스트가 부여한 임시 content URI 읽기 권한만 받으며 파일 시스템 경로를 보지 않고 책을 저장소에 쓰지 않습니다.
- 엄격한 봉투: Explorer Action 요청은 정확히 하나의 EPUB 대상, 상위 폴더, 일치하는 프로토콜 버전, 지원되는 호스트 빌드, 두 가지 읽기 권한을 모두 담아야 합니다. 그 외는 파일을 열기 전에 거부됩니다.
- 다른 앱을 위한 별도 진입점: `ACTION_VIEW`는 별도로 내보낸 액티비티가 받으며 읽기 권한이 있는 `content://` 문서만 받습니다 (`file://`도 디렉터리도 안 됩니다). Explorer Action 액티비티는 계속 AutoJs6 플러그인 권한으로 보호됩니다. 보낸 앱의 권한은 `최근 책에 추가`를 선택할 때만 유지됩니다.
- 보호된 스크립트 서비스: `org.autojs.plugin.EPUB` 서비스는 플러그인 권한 뒤에서 내보내지고, 서명이 일치하는 AutoJs6 호스트 패키지에만 응답하며, 모든 요청을 고정된 상한 (href 길이, 텍스트 창, 검색 페이지, 옵션 크기, 동시에 열린 8권, 리소스당 64 MB)으로 검사하고, 백그라운드에서 리더를 시작하지 않습니다. 리더 세션은 호스트에 일회용 토큰만 건네고, 리더 Activity는 호스트가 직접 시작하며, 아무도 받지 않은 세션은 60초 뒤 닫히고, 잘못된 토큰으로는 아무것도 열리지 않습니다.
- 요청 시에만 업데이트 확인: `업데이트 확인`을 탭할 때만 설정 페이지가 HTTPS로 GitHub Releases API에 묻고 (하루 최대 한 번, 리디렉션 없음, 응답 크기 제한), 결과를 보여 준 뒤 릴리스 페이지를 브라우저에서 엽니다. 플러그인이 스스로 무언가를 내려받거나 설치하지 않습니다.
- 제한된 파싱: 손상된 컨테이너 (ZIP이 아님, `container.xml` 없음, 패키지 문서 없음, manifest 경로 이탈)는 충돌 대신 오류 메시지로 끝납니다.
- 외부 링크는 전체 주소를 표시하고 확인 후에만 시스템 브라우저로 엽니다. `http`와 `https` 이외의 스킴은 거부됩니다.
- 읽기 데이터는 기기 안에 남습니다: 위치는 내용 지문을 키로 저장되며 파일 경로나 이름은 저장소에 기록되지 않습니다.
- 읽어 주기는 내보내지 않는 미디어 재생 서비스에서 실행되며, 음성이 읽는 동안에만 존재하고 중지하거나 책이 끝나거나 리더를 닫으면 멈춥니다 (`백그라운드에서 계속`이 켜져 있으면 알림에서 중지할 때 멈춥니다). 텍스트는 시스템 설정에서 선택한 텍스트 음성 변환 엔진으로 전달되며, 플러그인은 웨이크 락을 보유하지 않습니다.

매니페스트는 네트워크 권한, AutoJs6 플러그인 권한, 그리고 읽어 주기를 위한 포그라운드 서비스 권한 (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`)과 Android 13+의 `POST_NOTIFICATIONS` (읽어 주기를 시작할 때 한 번만 요청하며, 거부해도 알림 제어 버튼 없이 읽어 주기가 계속됨)을 요청합니다. AndroidX는 내보내지 않는 동적 리시버를 보호하는 패키지 한정 서명 권한을 추가하지만 기기 데이터 접근을 부여하지 않습니다. 저장소, 미디어, 카메라, 위치, 접근성, 오버레이 권한은 요청하지 않습니다.

******

### 플러그인 인터페이스

******

다음 정보는 개발자용입니다. 호스트는 다음 식별 정보로 플러그인을 검색하고 실행합니다:

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

Explorer Action v2는 단일 파일의 기본 버튼과 메뉴를 지원하며 문서와 상위 폴더의 임시 읽기 권한을 사용합니다. AutoJs6 빌드 5269 이상이 필요합니다.

- [Explorer Action 호환성 매트릭스 보기](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/explorer-action-compatibility.md)

******

### 로드맵

******

ROADMAP.md는 모든 마일스톤을 수용 기준과 증거가 있는 체크리스트로 추적합니다: P0부터 P9까지 (리더, 환경설정과 글꼴, 검색과 북마크, 소리 내어 읽기, 독립 진입점, 호스트 계약, `epub` 스크립트 API, 견고성, 1.0.0 릴리스 게이트, 1.1.0의 하이라이트, 메모, 내보내기)는 체크되었습니다. 체크되지 않은 항목은 출시된 기능이 아니라 계획입니다. Issues를 통한 피드백을 환영합니다.

- [ROADMAP.md 보기](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/ROADMAP.md)

******

### 릴리스 기록

******

#### v1.1.0

_2026/09/21_

- `힌트` 1.1.0은 로드맵 P9 (하이라이트, 메모, 내보내기) 를 완료합니다: 리더는 선택한 텍스트를 하이라이트하고 메모를 달며, 하이라이트를 패널에 나열하고 Markdown 으로 내보냅니다. EPUB 계약 버전 2 덕분에 이를 지닌 호스트는 `book.annotations()` 로 하이라이트를 읽고 `highlight` 이벤트를 받으며, AutoJs6 6.8.0 (빌드 5282) 은 계약 버전 1 로 계속 동작합니다
- `기능` 리더 안의 하이라이트와 메모 (로드맵 P9.2): 텍스트 선택 도구 모음에서 마지막으로 고른 색으로 선택 구간을 하이라이트하거나 메모 편집기 (하이라이트 / 밑줄, 5 가지 색, 메모) 를 열 수 있습니다. 하이라이트는 페이지에 렌더링되며 탭하면 편집기가 다시 열립니다. "하이라이트와 메모" 패널은 장별로 읽기 순서대로 나열하며 이동 / 편집 / 삭제 / 모두 지우기를 지원합니다. 설정 페이지에서는 저장된 하이라이트를 표시하고 지울 수 있습니다
- `기능` 책의 하이라이트와 메모를 Markdown 으로 내보내기 (로드맵 P9.3): 패널의 내보내기 버튼으로 시스템 공유 시트를 통해 텍스트를 공유하거나 문서 선택기에서 고른 위치에 `.md` 파일을 저장할 수 있습니다. 문서에는 제목, 저자, 읽기 순서의 각 장과 인용문, 메모, 시각이 담깁니다
- `기능` 호스트용 EPUB 계약 버전 2 (로드맵 P9.4): `IEpubBook.getAnnotations` 가 책의 하이라이트와 메모를 읽기 순서로 페이지 단위로 나열하고, 리더 세션은 하이라이트가 추가, 편집, 삭제될 때 `highlight` 이벤트를 보냅니다. 플러그인은 공개 기준선을 계약 버전 1 로 유지하고 `epubMaxContractVersion` 에 버전 2 를 표시하며, 각 책과 세션에 호스트의 열기 요청이 담은 버전으로 응답합니다. 버전 1 호스트 (AutoJs6 6.8.0 build 5282) 에는 변화가 없습니다
- `의존성` `androidx.room:room-runtime` 2.8.1 추가 (하이라이트와 메모 데이터베이스, 로드맵 D4 / P9). `room-compiler` 는 빌드 시 KSP 를 통해서만 실행됩니다
- `의존성` `epub-api.aar` 를 EPUB 계약 버전 2 의 릴리스 빌드로 업그레이드 (호스트 모듈 `plugin-api/epub-api`. 원본 호스트 커밋과 다이제스트는 `libs/README.md` 와 `locks/host-api-aars.lock` 에 기록)

#### v1.0.0

_2026/09/21_

- `힌트` 첫 릴리스: 1.0.0은 로드맵 P0부터 P8까지 (리더, 환경설정과 글꼴, 검색과 북마크, 소리 내어 읽기, 독립 진입점, 호스트 계약, `epub` 스크립트 API, 견고성과 릴리스 게이트)를 마쳤습니다. `epub` 스크립트 API와 샘플 스크립트는 AutoJs6 6.8.0 (빌드 5282)과 함께 제공되며, 하이라이트, 메모, 내보내기는 1.1.0 (로드맵 P9)에서 이어집니다
- `기능` AutoJs6 파일 관리자에서 `.epub` 파일에 `EPUB 읽기` 기본 버튼과 메뉴 동작 제공 (플러그인 ID `readium-epub-reader`, Explorer Action v2); 호스트가 `application/zip`으로 보고하는 `.epub` 확장자 파일도 허용됩니다
- `기능` 리더 기반: EPUB 2와 EPUB 3 책을 Readium 내비게이터로 렌더링하고 목차와 확인이 필요한 외부 링크를 제공
- `기능` 읽기 위치 기억: 각 책의 마지막 위치를 내용 지문 (열 때는 빠른 키, 이후 전체 파일 SHA-256) 아래에 저장하고 다음에 열 때 복원합니다; `처음부터 읽기`로 지울 수 있습니다
- `기능` 리더 화면: 툴바에 책 제목과 현재 챕터, 합성 위치와 백분율을 보여 주는 진행률 표시줄, 가운데 탭으로 몰입 모드, 탭 영역과 볼륨 키로 페이지 넘기기, 스크롤 모드 전환
- `기능` 읽기 설정 패널: 글자 크기, 글꼴, 줄 간격, 페이지 여백, 문단 간격, 정렬, 하이픈 연결, 출판사 스타일, 단 수, 페이지 / 스크롤 레이아웃이 즉시 적용되고 모든 책에서 기억됩니다; 라이트, 세피아, 다크 테마와 `호스트 따르기`, 툴바와 시스템 바 색상도 테마에 맞춰 바뀝니다
- `기능` 글꼴 가져오기: 시스템 문서 선택기로 고른 TTF / OTF 파일을 검증한 뒤 (SFNT 서명, `name` 테이블, 파일당 20 MB, 최대 10개) `files/fonts/<sha256>`에 비공개로 저장하고 `@font-face` 선언으로 Readium 내비게이터에 제공합니다; 가져온 글꼴은 읽기 설정 패널의 기본 글꼴 뒤에 나열되며 패널에서 삭제할 수 있습니다
- `기능` CJK 세로쓰기와 오른쪽에서 왼쪽으로 읽는 책: 읽기 진행 방향은 출판물을 따르고 (오른쪽에서 왼쪽 책에서는 탭 영역이 반전), 페이지 진행이 오른쪽에서 왼쪽인 일본어 / 중국어 책은 Readium CSS로 세로쓰기 표시되며, `글자 방향` 설정으로 가로쓰기 또는 세로쓰기를 강제할 수 있고, 인터페이스의 레이아웃 방향은 책과 독립적입니다
- `기능` 고정 레이아웃 책: 진행 표시줄에 `x / N 페이지`를 표시하고, `펼침 보기` 설정 (자동 = 가로 방향에서 두 페이지, 한 페이지, 두 페이지)을 제공하며, 고정 레이아웃에 적용되지 않는 글자 설정을 숨기고, 손가락 확대는 Readium 내장
- `기능` 전체 텍스트 검색: 도구 모음의 `검색` 항목이 결과 패널을 열어 50개씩 (최대 500개) 장별로 묶어 문맥과 함께 표시하고, 결과를 탭하면 해당 위치로 이동하며 페이지의 일치 항목을 강조하고, 진행 표시줄 위에 이전 / 다음을 제공합니다
- `기능` 북마크: 도구 모음 아이콘이 현재 페이지에 북마크를 추가하거나 제거하고 (장 이름과 본문 발췌 포함), `북마크` 패널이 최신순으로 나열하여 이동, 삭제, 모두 지우기를 제공합니다. 책마다 (최대 500개) 읽기 위치와 함께 저장됩니다
- `기능` 읽기 조작: 탭으로 페이지 넘기기를 끄거나 좌우 / 상하로 설정할 수 있고, 하드웨어 키보드의 방향키, 페이지 키, 스페이스로 페이지를 넘기며, 선택한 텍스트에 복사, 공유, 웹 검색과 시스템의 텍스트 처리 앱을 제공합니다
- `기능` 링크: 책 안의 링크는 리더 안에서 이동하고 뒤로 키로 이동 전 위치로 돌아가며, 각주와 미주는 대화 상자에 표시되고, 외부 링크는 확인 후 또는 설정에 따라 바로 브라우저에서 열립니다. 다른 스킴의 링크는 거부합니다
- `기능` 이미지: 이미지를 탭하면 전체 화면으로 열리고 캡션이 표시됩니다
- `기능` 읽어 주기: 더보기 메뉴에서 시스템 텍스트 음성 변환 엔진으로 현재 페이지부터 책을 읽어 주고, 읽고 있는 문장을 강조하며 페이지를 자동으로 넘깁니다. 페이지 아래의 바와 미디어 알림에서 재생 / 일시정지, 이전 / 다음 문장, 중지를 조작할 수 있고, 헤드셋 버튼이 동작하며, 속도, 음높이, 언어, 음성을 조정할 수 있고, 화면이 꺼져도 읽어 주기가 계속되며, 리더를 닫으면 중지됩니다
- `기능` 읽어 주기 수면 타이머 (15 / 30 / 60분 또는 이 장의 끝), 읽어 주는 동안 화면 켜짐 유지 스위치, `백그라운드에서 계속` (기본 꺼짐): 켜면 리더를 닫아도 책이 끝나거나 타이머가 끝날 때까지 계속 읽어 주고, 알림에서 일시정지 / 중지하거나 읽고 있는 문장에서 책을 다시 열 수 있으며, 같은 책을 다시 열면 읽어 주던 위치를 이어받습니다. 백그라운드 읽어 주기가 멈추면 읽기 위치를 저장합니다
- `기능` 책은 호스트가 부여한 파일 디스크립터로 위치 기반으로 제자리에서 읽으며 저장소에 복사하거나 압축을 풀지 않음
- `기능` 인터페이스, 설명, README, changelog를 10개 언어로 제공
- `기능` 독립 실행 런처: 앱 아이콘이 최근 책 그리드 (표지, 제목, 저자, 진행률, 마지막 읽은 시각, 최대 100권)와 시스템 문서 선택기로 책을 고르는 `EPUB 열기` 버튼을 엽니다. 고른 책은 영구 읽기 권한을 유지해 그리드에서 다시 열 수 있고, 파일이 사라진 책은 사용할 수 없음으로 표시되며, 길게 누르면 책을 제거하고 권한을 해제합니다
- `기능` 다른 앱에서 열기: 파일 관리자, 브라우저, 메일 앱이 `ACTION_VIEW`로 `content://` EPUB을 리더에 넘길 수 있습니다. 책은 평소처럼 열리지만, 오버플로 메뉴의 `최근 책에 추가`로 보낸 앱의 접근 권한을 유지할 수 있는 경우가 아니면 런처에 나열되지 않습니다 (유지할 수 없으면 거부합니다). `file://` 경로, 읽기 권한이 없는 요청, 디렉터리는 거부됩니다
- `기능` 설정 페이지와 릴리스 기록: 런처 메뉴와 리더 오버플로 메뉴에서 설정 페이지를 열어 테마, 탭 영역, 볼륨 키 페이지 넘기기, 소리 내어 읽기의 속도, 높낮이, 기본 취침 타이머, 외부 링크, 플러그인이 보관하는 데이터 (읽기 위치, 최근 책, 가져온 글꼴, 환경설정. 모두 확인 후 지움)를 설정할 수 있습니다. 정보 섹션, 내장 릴리스 기록, 수동 업데이트 확인 (탭할 때만 GitHub에 묻고 릴리스 페이지를 브라우저에서 열며 아무것도 내려받지 않음. `이 버전 무시`는 기억됨)도 제공합니다
- `기능` AutoJs6 호스트를 위한 EPUB 기능 서비스 (로드맵 P5.2): `org.autojs.plugin.EPUB` Binder 서비스가 호스트의 읽기 전용 디스크립터에서 책을 열고 메타데이터, 목차, 읽기 순서, 장 텍스트 (일반 텍스트 또는 경량 Markdown, 페이지 단위), 파이프를 통한 리소스, 전체 텍스트 검색, 위치 수를 돌려줍니다. 동시에 최대 8권까지 열 수 있고, 5분간 사용하지 않은 책은 자동으로 닫히며, 모든 요청은 경계 검사를 거치고, 서비스는 AutoJs6 호스트만 호출할 수 있습니다
- `기능` EPUB 계약 기반의 호스트 리더 세션 (로드맵 P5.3): `openReader`는 책을 열고 일회용 세션 토큰을 발급한 뒤 실행을 호스트에 맡기며, 호스트가 그 토큰을 담아 리더 Activity를 명시적으로 시작합니다. 이후 세션은 하나의 generation과 엄격히 증가하는 순번으로 `open`, `progress` (최소 500 ms 간격), `bookmark`, `error`, `close` 이벤트를 보고하고, `goTo` (로케이터, href 또는 진행률), `navigate` (페이지 또는 장), `setPreferences` (계약이 정한 설정 부분집합; 알 수 없는 키는 보고만 하고 적용하지 않음), `getBookmarks`, `getState`를 받으며, 새 세션은 이전 세션을 대체하고, 60초 안에 리더가 받지 않으면 자동으로 닫히며, 호스트의 `close`는 명시적으로 요청하지 않는 한 리더를 닫지 않습니다
- `수정` 공유 빌드 플러그인 1.8.3을 통해 AGP 9.1의 SDK XML v4 파싱 경고 및 JVM 단위 테스트 조립 작업에서 APK 네이티브 라이브러리 정렬 검사가 잘못 실행되는 문제 해결
- `수정` 읽기 진행 기록 저장에 실패해도 (책 디렉터리가 제거됨, 저장소에 쓸 수 없음) 리더가 더 이상 중단되지 않고, 해당 기록만 잃은 채 계속 읽습니다
- `수정` 설정 제공자를 읽는 동안 호스트 AutoJs6 가 중지되거나 업데이트되어도 리더가 함께 종료되지 않습니다. 해당 읽기만 실패하고 호스트의 언어 / 야간 모드는 적용되지 않습니다
- `수정` 호스트 세션의 실행 인텐트가 태스크 최상단에 이미 있는 리더에 도착하면 (single-top 전달, 예를 들어 스크립트가 리더를 열어 둔 뒤) 이제 60초 타임아웃까지 미인수 상태로 기다리는 대신 새 리더 인스턴스에서 열립니다. 이전 리더는 교체될 때와 같이 종료됩니다 (로드맵 P6.2)
- `수정` NCX / OPF XML이 잘렸거나 형식이 잘못된 책은 이제 안전하게 실패합니다: 서비스는 `PARSE_FAILED`를 답하고 리더는 열기 실패 패널을 표시합니다. 이전에는 `INTERNAL` 코드이거나 Readium XML 파서가 던지는 `AssertionError`로 인한 충돌이었습니다 (로드맵 P7.1)
- `수정` `search` 한 페이지는 플러그인 쪽에서 50초로 제한되며, 거대한 책 (50 000개 리소스) 의 뒷부분에서만 일치하는 쿼리에는 `TIMEOUT`을 답합니다. Binder 스레드가 호스트 자체의 60초 호출 타임아웃을 지나서도 바쁜 상태로 남지 않습니다 (로드맵 P7.1)
- `수정` 리더에서 Readium이 만드는 모든 페이지 WebView는 이제 Readium 자체 설정 위에 경계를 가집니다: 파일 시스템과 콘텐츠 제공자 접근을 막고, file URL의 두 교차 출처 스위치를 끄며, JavaScript는 Readium을 위해 켜진 채로 둡니다 (로드맵 D6). WebView, 컨테이너, 컴포넌트 경계 검토는 `docs/dev/security-boundaries.md`에 기록되어 있습니다 (로드맵 P7.2)
- `수정` 리더 프로세스가 처리되지 않은 예외로 죽을 때, 시스템 자체의 충돌 처리가 실행되기 전에 현재 읽기 위치를 먼저 동기적으로 디스크에 씁니다. 플러그인 자체는 로그를 쓰지 않고 Timber 트리도 심지 않으므로 책 제목, 경로, 본문이 logcat에 나타나지 않습니다 (로드맵 P7.7)
- `수정` TrueType / OpenType 컬렉션 (`.ttc` / `.otc`)을 읽기 글꼴로 고르면 이제 파일을 글꼴이 아니라고 하는 대신 글꼴 컬렉션은 지원하지 않는다고 알립니다. `docs/dev/compatibility-matrix.md`에 기록한 기기 x 시나리오 호환 매트릭스를 돌리며 찾은 문제입니다 (로드맵 P7.3)
- `수정` 접근성: 읽기 환경설정 패널의 슬라이더 네 개 (글자 크기, 페이지 여백, 줄 높이, 단락 간격)에 화면 낭독기가 읽을 수 있는 레이블이 붙었고, 리더 도구 모음은 시스템 큰 글꼴에서 높이가 늘어나 챕터 부제를 잘라내지 않습니다. 레이블, 48 dp 터치 대상, 1.3배 글꼴 배율, 야간 모드, 강제 RTL, 키보드 페이지 넘김, 가로 화면을 다루는 instrumentation 감사가 근거입니다 (로드맵 P7.6)
- `수정` 소리내어 읽기는 초기화를 끝내지 못하는 음성 엔진 (API 24 에뮬레이터의 음성 데이터 없는 Google TTS가 바로 그렇습니다)을 더 이상 무한정 기다리지 않습니다. 20초 뒤 리더는 사용할 수 있는 엔진이 없다고 알리고 대기 상태로 돌아가며, 그 뒤에 도착하는 세션은 닫습니다 (로드맵 P7.3)
- `개선` 릴리스 APK 크기: Readium이 내비게이터 애셋에 함께 넣는 DiViNa 플레이어 (427 KB, EPUB 리더에서는 쓰이지 않음) 를 병합 애셋에서 제외하고 플러그인 패키지 전체 keep 규칙도 없앴으므로 R8이 플러그인 자체 클래스도 줄입니다. 릴리스 APK는 P5 이후 3,922,786 B에서 3,328,220 B가 되었습니다 (로드맵 P7.5, 자세한 내용은 `docs/dev/release-size.md`)
- `의존성` Readium Kotlin Toolkit 3.4.0 추가 (`readium-shared`, `readium-streamer`, `readium-navigator`, `readium-navigator-media-tts`)
- `의존성` `androidx.media3:media3-session` 1.11.0 추가 (`readium-navigator-media-tts`가 이미 간접적으로 가져옴. 읽어 주기 포그라운드 서비스를 위해 직접 선언)
- `의존성` `org.jsoup:jsoup` 1.23.2 추가 (`readium-shared`가 이미 간접적으로 가져옴. EPUB 서비스의 장 텍스트 추출을 위해 직접 선언)

##### 더 많은 릴리스 기록

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/app/src/main/assets/doc/CHANGELOG-ko.md)

******

### 빌드

******

```powershell
.\gradlew.bat :app:assembleDebug
```

Release 빌드:

```powershell
.\gradlew.bat :app:assembleRelease
```

빌드 매개변수는 `version.properties`에서 가져옵니다. 현재 최소 SDK는 24이고 대상 SDK는 37입니다.

******

### 현지화와 문서 생성

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

`strings.xml`은 플러그인 정보와 리더 UI를 현지화하고 `plugin_instruction.md`는 호스트에 표시되는 사용 설명을 제공합니다. README와 changelog는 반드시 `.readme/`와 `.changelog/`의 JSON 소스를 수정한 뒤 `py .python/generate_markdown.py`를 실행해 다시 생성하며, 생성물은 손으로 편집하지 않습니다. `py .python/generate_markdown.py --check`로 소스와 생성물의 동기화를 검증할 수 있습니다.

******

### 라이선스와 서드파티 고지

******

플러그인은 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/LICENSE)으로 배포됩니다. Readium Kotlin Toolkit (BSD 3-Clause), AndroidX Media3와 Jsoup, AutoJs6 계약 라이브러리, 그리고 APK에 포함된 다른 구성 요소의 버전, 체크섬, 라이선스는 [서드파티 고지](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/THIRD_PARTY_NOTICES.md)에 나열되어 있습니다.

******

### 링크

******

- AutoJs6 문서: https://docs.autojs6.com
- `epub` 스크립트 API 참조: https://docs.autojs6.com/#/epub
- EPUB 3.3 명세: https://www.w3.org/TR/epub-33/
- Readium Kotlin Toolkit: https://github.com/readium/kotlin-toolkit
- 서드파티 고지: https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/THIRD_PARTY_NOTICES.md
- 16 KB 페이지 정렬과 빌드 검증: https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/16kb.md
