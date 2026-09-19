******

### 릴리스 기록

******

# v1.0.0

###### 2026/09/19

* `힌트` 개발 빌드: 로드맵 P0 단계 (스켈레톤, Readium 검증, 테스트 픽스처)가 진행 중입니다. 첫 공개 릴리스는 로드맵 P8 단계에서 제공됩니다
* `기능` AutoJs6 파일 관리자에서 `.epub` 파일에 `EPUB 읽기` 기본 버튼과 메뉴 동작 제공 (플러그인 ID `readium-epub-reader`, Explorer Action v2); 호스트가 `application/zip`으로 보고하는 `.epub` 확장자 파일도 허용됩니다
* `기능` 리더 기반: EPUB 2와 EPUB 3 책을 Readium 내비게이터로 렌더링하고 목차와 확인이 필요한 외부 링크를 제공
* `기능` 책은 호스트가 부여한 파일 디스크립터로 위치 기반으로 제자리에서 읽으며 저장소에 복사하거나 압축을 풀지 않음
* `기능` 인터페이스, 설명, README, changelog를 10개 언어로 제공
* `의존성` Readium Kotlin Toolkit 3.4.0 추가 (`readium-shared`, `readium-streamer`, `readium-navigator`, `readium-navigator-media-tts`)
