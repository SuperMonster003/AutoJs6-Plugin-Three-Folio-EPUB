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

> 현재 단계 (1.0.0 개발 빌드): 리더는 Readium 기본 설정으로 책을 열고 목차를 제공합니다. 읽기 위치 기억, 북마크, 환경 설정, 전체 텍스트 검색, 읽어주기, 고정 레이아웃, 글꼴 가져오기, 독립 런처 진입점과 `epub` 스크립트 API는 ROADMAP.md에 계획되어 있으며 아직 사용할 수 없습니다.

******

### 주요 기능

******

- Readium 엔진: EPUB 2 (NCX)와 EPUB 3 (NAV) 책을 Readium 내비게이터와 Readium CSS로 렌더링하며 내부 링크, 각주, 이미지를 지원합니다.
- 복사 없음: EPUB 컨테이너를 읽기 전용 디스크립터에서 위치 기반으로 읽으므로 큰 책도 캐시 파일 없이 열립니다.
- 목차: 도구 모음에서 원하는 장으로 이동하며 중첩 항목은 깊이를 유지합니다.
- 외부 링크: `http` 또는 `https` 링크를 탭하면 전체 주소를 표시하고 확인 후에만 시스템 브라우저를 엽니다.
- 호스트 연동: 메뉴와 대화 상자는 AutoJs6의 언어와 다크 모드를 따르며, Explorer Action 봉투는 콘텐츠를 열기 전에 엄격하게 검증됩니다.
- 다국어: 인터페이스, 설명, README, changelog를 10개 언어로 제공합니다.

******

### 사용 방법

******

1. [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases) 페이지에서 최신 플러그인 APK를 내려받아 기기에 설치합니다.
2. AutoJs6의 플러그인 센터를 열고 `Readium EPUB Reader` 플러그인을 활성화합니다.
3. AutoJs6 파일 관리자에서 `.epub` 파일을 탭하거나 메뉴 (더 보기)를 열어 `EPUB 읽기`를 선택합니다.
4. 도구 모음의 목차 버튼으로 장 사이를 이동하고, 뒤로 키로 리더를 닫습니다.

> 플러그인 센터에 플러그인이 보이지 않으면 먼저 AutoJs6를 최신 버전 (내부 빌드 5269 이상)으로 업데이트하세요. Explorer Action v2는 단일 파일의 기본 버튼과 메뉴를 지원하며 문서와 상위 폴더의 임시 읽기 권한을 사용합니다.

******

### 지원 형식

******

플러그인은 다음 확장자를 인식하며 호스트가 `application/epub+zip`으로 명시한 확장자 없는 파일도 허용합니다:

```text
epub
```

EPUB만 지원합니다: EPUB 2 또는 EPUB 3의 리플로우와 고정 레이아웃 책. 만화 아카이브 (CBZ), 오디오북, PDF, LCP로 보호된 책은 범위 밖입니다. LCP 암호화로 표시된 책은 깨진 내용을 표시하지 않고 읽을 수 없다고 알립니다.

******

### 자주 묻는 질문

******

#### 왜 읽기 위치가 아직 기억되지 않나요?

읽기 위치 기억과 북마크는 ROADMAP.md의 다음 마일스톤입니다. 현재 빌드는 항상 책의 처음부터 엽니다.

#### 글꼴, 글자 크기, 테마를 바꿀 수 있나요?

아직은 아닙니다. 읽기 설정 (글꼴, 크기, 줄 간격, 여백, 테마, 페이지 또는 스크롤 모드)은 환경 설정 마일스톤에서 제공됩니다. 현재 빌드는 Readium 기본값을 사용합니다.

#### 이 플러그인이 내 책을 어딘가로 업로드하나요?

아니요. 플러그인에는 자체 서버가 없습니다. 네트워크는 책 자체가 원격 리소스를 참조할 때와 독립 설정 페이지에 계획된 수동 업데이트 확인에만 사용됩니다.

******

### 권한과 보안

******

플러그인은 책 콘텐츠에 대해 Readium의 기본 동작을 유지합니다. 책 안의 스크립트와 원격 리소스는 제거되거나 차단되지 않으며 평문 `http://` 리소스도 포함됩니다. 신뢰할 수 있는 책만 여세요.

- 최소 권한: 플러그인은 호스트가 부여한 임시 content URI 읽기 권한만 받으며 파일 시스템 경로를 보지 않고 책을 저장소에 쓰지 않습니다.
- 엄격한 봉투: Explorer Action 요청은 정확히 하나의 EPUB 대상, 상위 폴더, 일치하는 프로토콜 버전, 지원되는 호스트 빌드, 두 가지 읽기 권한을 모두 담아야 합니다. 그 외는 파일을 열기 전에 거부됩니다.
- 제한된 파싱: 손상된 컨테이너 (ZIP이 아님, `container.xml` 없음, 패키지 문서 없음, manifest 경로 이탈)는 충돌 대신 오류 메시지로 끝납니다.
- 외부 링크는 전체 주소를 표시하고 확인 후에만 시스템 브라우저로 엽니다. `http`와 `https` 이외의 스킴은 거부됩니다.

매니페스트는 네트워크 권한과 AutoJs6 플러그인 권한만 요청합니다. AndroidX는 내보내지 않는 동적 리시버를 보호하는 패키지 한정 서명 권한을 추가하지만 기기 데이터 접근을 부여하지 않습니다. 저장소, 미디어, 카메라, 위치, 접근성, 오버레이 권한은 요청하지 않습니다.

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

계획된 기능과 완료 상태는 ROADMAP.md에 체크 가능한 목록으로 기록되며 수락 기준이 있는 마일스톤으로 정리됩니다: 읽기 위치 기억과 북마크, 환경 설정과 글꼴 가져오기, 전체 텍스트 검색, 읽어주기, 고정 레이아웃, 독립 앱 진입점, 호스트 계약과 `epub` 스크립트 API. 체크되지 않은 항목은 출시된 기능이 아니라 계획입니다. Issues를 통한 피드백을 환영합니다.

- [ROADMAP.md 보기](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/ROADMAP.md)

******

### 릴리스 기록

******

#### v1.0.0

_2026/09/19_

- `힌트` 개발 빌드: 로드맵 P0 단계 (스켈레톤, Readium 검증, 테스트 픽스처)가 진행 중입니다. 첫 공개 릴리스는 로드맵 P8 단계에서 제공됩니다
- `기능` AutoJs6 파일 관리자에서 `.epub` 파일에 `EPUB 읽기` 기본 버튼과 메뉴 동작 제공 (플러그인 ID `readium-epub-reader`, Explorer Action v2)
- `기능` 리더 기반: EPUB 2와 EPUB 3 책을 Readium 내비게이터로 렌더링하고 목차와 확인이 필요한 외부 링크를 제공
- `기능` 책은 호스트가 부여한 파일 디스크립터로 위치 기반으로 제자리에서 읽으며 저장소에 복사하거나 압축을 풀지 않음
- `기능` 인터페이스, 설명, README, changelog를 10개 언어로 제공
- `의존성` Readium Kotlin Toolkit 3.4.0 추가 (`readium-shared`, `readium-streamer`, `readium-navigator`, `readium-navigator-media-tts`)

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

### 링크

******

- AutoJs6 문서: https://docs.autojs6.com
- EPUB 3.3 명세: https://www.w3.org/TR/epub-33/
- Readium Kotlin Toolkit: https://github.com/readium/kotlin-toolkit


[16 KB page alignment and build verification](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/16kb.md)
