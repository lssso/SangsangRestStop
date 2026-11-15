# 상상휴게소 (SangsangRestStop)

> **차 안에서 화면 대신 대화로, 이동 시간을 상상 놀이 시간으로.**

상상휴게소는 어린이와 가족이 차량 안에서 화면을 계속 바라보지 않고, 듣고 말하며 함께 즐길 수 있도록 기획한 음성 중심 놀이 앱입니다.

일방적으로 영상을 시청하는 대신, 아이가 직접 단어를 떠올리고 힌트를 추리하며 소리에 귀 기울이는 놀이 경험을 제안합니다. 차량 환경을 고려해 간결한 화면, 친근한 캐릭터, 짧은 음성 안내를 중심으로 설계했습니다.

- **팀:** 한지붕 상상연구소
- **대상:** Hyundai Pleos Connect Android 앱 공모전
- **현재 상태:** 단어 짝짝꿍과 짜잔! 누구?는 게임 엔진·화면·STT/TTS 연결 코드가 구현됐으며, 쫑긋 퀴즈는 준비중입니다.

상상휴게소는 별도 Git 이력과 원격 저장소를 사용하는 독립 프로젝트입니다. 분리 원본 프로젝트의 코드와 리소스는 이 저장소에서 변경하지 않습니다.

## 핵심 특징

- **음성 중심 놀이 — 서비스 목표:** 화면을 지속적으로 바라보지 않고 듣고 말하며 참여하는 경험을 지향합니다.
- **어린이 친화 UX — 서비스 목표:** 큰 선택 영역, 친근한 캐릭터와 짧고 쉬운 안내를 중심으로 설계했습니다.
- **가족 참여 — 서비스 목표:** 아이와 보호자가 함께 단어를 떠올리고 정답을 추리하는 대화형 놀이를 제안합니다.
- **개인정보 최소화 — 설계 원칙:** 음성 녹음본과 위치·이동 이력을 불필요하게 저장하지 않습니다.
- **게임 확장 구조 — 현재 구현:** 공통 `SpeechInput`·`SpeechOutput` 경계와 독립적인 순수 Kotlin 게임 엔진을 사용합니다.

주행 중 사용 허용 범위와 전화·내비·시동 등 차량 이벤트 연동은 Pleos 공식 정책 및 실제 환경 검증 전이며, 지원 완료로 표시하지 않습니다.

## 공모전 MVP

1. **단어 짝짝꿍**
   - 기존 WordChain 엔진과 설정·진행 화면 재사용
   - 마지막 글자로 단어를 이어 말하는 놀이
2. **짜잔! 누구?**
   - 앱이 힌트를 순차적으로 제공
   - 사용자가 정답을 음성으로 맞히는 게임
3. **쫑긋 퀴즈**
   - 동물·탈것·악기 소리를 듣고 정답을 맞히는 게임

| 게임 | 놀이 방식 | UI | 엔진 | 음성 연결 |
| --- | --- | --- | --- | --- |
| 단어 짝짝꿍 | 단어를 이어 말하는 끝말잇기 | 구현 | 구현 | 코드 연결 |
| 짜잔! 누구? | 단계별 힌트로 정답 추리 | 구현 | 구현 | 코드 연결 |
| 쫑긋 퀴즈 | 동물·탈것·악기 소리 맞히기 | 미구현 | 미구현 | 미구현 |

### 게임별 구현 메모

- **단어 짝짝꿍:** WordChain 규칙·상태 엔진, 설정·진행 UI, 난이도·힌트 설정 저장과 단위 테스트가 구현돼 있습니다. TTS 안내가 끝난 뒤 코드로 합성한 마이크 활성화 안내음을 재생하고 STT 청취를 시작합니다.
- **짜잔! 누구?:** 주제 3종 × 난이도 3종의 로컬 문제 데이터, 문제별 단계형 힌트, 정답·동의어 판정, 오답·무응답 처리와 게임 상태 연결이 구현돼 있습니다. 현재 개발 기본값은 한 판 5문제, 문제당 힌트 4개이며 기획 검수가 남아 있습니다.
- **쫑긋 퀴즈:** 대시보드 준비중 카드만 제공하며 엔진·화면·문제 데이터·음원은 아직 구현하지 않았습니다.

단어 짝짝꿍과 짜잔! 누구?의 STT/TTS **코드 연결은 완료**됐지만, 현재 Release APK의 Pleos 설치·실행이나 실차 음성 동작 검증 완료를 의미하지 않습니다.

현재 MVP에 포함되지 않는 이전 기획 시안은 `docs/design/references`의 Legacy 참고 자료로만 보존합니다.

## 제출용 APK

[서명된 Release APK 다운로드](<apk/한지붕 상상연구소_상상휴게소_release.apk>)

```text
apk/한지붕 상상연구소_상상휴게소_release.apk
```

확인된 사실:

- Release 변형 APK
- 패키지명: `com.sangsangreststop.app`
- APK Signature Scheme v2 서명 검증 완료

검증 범위:

- `:app:assembleDebug` 성공
- `:app:testDebugUnitTest` 76개 통과, 실패·건너뜀 없음
- 현재 Release APK의 Pleos 설치·실행과 기존 설치 앱 서명 호환성은 미검증

위 서명 결과는 2026-11-15 제출 직전 검증 기록입니다. 다시 검증하려면 Android SDK Build Tools의 `apksigner`로 다음 명령을 실행합니다.

```powershell
apksigner verify --verbose --print-certs "apk\한지붕 상상연구소_상상휴게소_release.apk"
```

키스토어와 비밀번호는 저장소에 포함하지 않습니다.

## 현재 구현 상태

### 구현 및 현재 저장소 검증 완료

- 상상휴게소 프로젝트명, 앱 이름, 런처 아이콘과 패키지 식별 정보
- `applicationId`, Android namespace, Kotlin 패키지: `com.sangsangreststop.app`
- Jetpack Compose 대시보드와 MVP 게임 카드 3종
- 상상휴게소 로고, 캐릭터 포함 배경, 카드 배경·아트워크의 `sr_` 앱 리소스
- 단어 짝짝꿍 설정·진행 화면과 순수 Kotlin WordChain 엔진·단위 테스트
- 단어 짝짝꿍 난이도·힌트 설정 DataStore 저장
- 짜잔! 누구? 설정·진행 화면, 난이도·주제 선택과 대시보드 → 설정 → 진행 화면 이동
- 짜잔! 누구? 로컬 문제·단계형 힌트 데이터, 순수 Kotlin 게임 엔진, 정답 판정과 단위 테스트
- 짜잔! 누구? 진행 UI의 실제 게임 상태 연결 및 SpeechInput / SpeechOutput 연동
- Pleos SpeechToText / TextToSpeech SDK 어댑터 코드
- 두 게임의 TTS 종료 → 코드 합성 마이크 안내음 → STT 시작 흐름
- Pleos TTS 실패 시 Android TTS 대체 구현과 STT 불가 시 직접 입력 경로
- 공통 팝업 UI와 Compose Preview
- 음성 기능 진단 화면
- 앱 백그라운드 전환 시 단어 게임 일시정지·재개
- 대시보드의 현재 목소리 표시 이름 `상상놀이친구`

### 부분 구현 또는 별도 검증 필요

- 과거 Pleos 에뮬레이터에서 앱 설치·대시보드·단어 짝짝꿍 설정·진행 화면 실행 및 말하기 → 듣기 상태 전환 확인
- 현재 Release APK의 Pleos/Android 14 설치·실행과 기존 설치 앱 서명 호환성 재검증
- 단어 짝짝꿍·짜잔! 누구?의 Pleos STT 실제 인식 정확도, 무응답 원인, 안내음 음량·출력 채널·오인식 여부
- TTS 실제 출력 품질
- 짜잔! 누구?의 문제 수·힌트 단계·난이도·종료 조건 기획 검수
- 공통 팝업의 전체 런타임 게임 흐름 연결
- 목소리 선택·저장 화면: 명칭만 확정됐으며 기능은 미구현
- 전화·내비·시동·오디오 포커스 차량 이벤트
- 주행 중 사용 범위와 UI 제한

### 미구현

- 쫑긋 퀴즈 엔진·화면·문제 데이터·라이선스가 확인된 음원
- 게임별 음성 온보딩과 완료 상태 저장
- 쫑긋 퀴즈 설정·진행 화면 전용 이미지

## 확정된 온보딩 목표

각 게임의 최초 실행은 다음 흐름을 목표로 합니다.

1. 상상놀이친구가 규칙 설명 여부를 묻고 음성 또는 큰 ‘응’·‘아니’ 버튼으로 선택
2. 규칙 설명 완료 또는 생략 후 연습 1회 여부를 별도로 선택
3. 연습 완료 또는 생략 후 본게임 시작

이후 실행은 기본적으로 본게임으로 바로 진입하되 규칙 다시 듣기와 다시 연습하기를 제공합니다. 인식 실패나 무응답을 ‘아니’로 간주하거나 단계를 자동 생략하지 않습니다.

이 온보딩 UI·상태·저장 기능은 **현재 미구현**입니다. WordChain의 기존 음성 명령 분류와 TTS→STT 처리 일부는 구현 참고 대상일 뿐, 공통 온보딩 구현 완료를 의미하지 않습니다.

## 최근 기준선 검증

2026-11-15 제출 직전 현재 저장소를 다시 검증했습니다.

- `:app:assembleDebug` 성공
- `:app:testDebugUnitTest` 76개 통과, 실패·건너뜀 없음
- 공통 팝업 6종 Compose Preview 렌더링 확인
- 서명된 Release APK의 패키지명과 v2 서명 확인
- 이번 검증 세션에는 실행 중인 기기가 없어 현재 APK 설치·실행은 미검증

2026-11-13 기준선 작업 기록:

- 미사용 레거시 실행 리소스 79개를 전수 조사 후 삭제
- 삭제 전 프로젝트 외부 백업 및 SHA-256 79/79 일치 확인
- 분리 원본 프로젝트의 대응 파일과 SHA-256 79/79 일치 확인
- 불필요해진 레거시 리소스 Git 제외 규칙 제거
- Debug clean build 성공
- 기존 단위 테스트 47개 통과
- Git 포함 후보 194개만 복사한 깨끗한 사본에서도 Debug build와 단위 테스트 성공
- 대시보드, 단어 짝짝꿍 설정·진행, 게임 종료·일시정지 팝업 Preview 확인
- Pleos 에뮬레이터에서 설치·실행과 주요 화면 전환 확인
- 첫 로컬 Commit과 GitHub `main` Push 완료

빌드·테스트·에뮬레이터 결과는 해당 기준선의 검증 기록입니다. 이후 코드 변경 시 다시 검증해야 합니다.

## 개발 환경

- Android / Kotlin / Jetpack Compose / Material 3
- 단일 Gradle 모듈: `:app`
- applicationId / namespace / Kotlin package: `com.sangsangreststop.app`
- minSdk 28, compileSdk 37, targetSdk 37
- Kotlin 2.2.10, Android Gradle Plugin 9.5.1
- Java 11 소스 호환성
- Pleos SpeechToText / TextToSpeech SDK 3.3.1

## 빌드와 테스트

Windows PowerShell에서 Android Studio JBR 또는 호환 JDK를 사용합니다.

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

Clean build가 필요하면 다음을 실행합니다.

```powershell
.\gradlew.bat :app:clean :app:assembleDebug
```

제출 APK의 경로, 확인된 서명 결과와 재검증 명령은 위의 **제출용 APK** 절을 참고하세요.

## 실행

1. Android Studio에서 저장소 루트를 엽니다.
2. Gradle Sync를 수행합니다.
3. 일반 Android 에뮬레이터 또는 공식 Pleos 테스트 환경을 선택합니다.
4. `app` 실행 구성을 실행합니다.

제출 APK를 직접 검증할 때는 기존 설치 앱과의 서명 충돌 여부를 먼저 확인한 뒤 설치합니다. 서명 충돌이 발생하면 기존 앱을 임의로 삭제하지 않습니다.

일반 Android, Pleos 에뮬레이터, 실제 차량의 검증 결과를 혼용하지 않습니다. 실제 차량 검증은 아직 완료되지 않았습니다.

## 저장소 구조

```text
app/
  src/main/java/com/sangsangreststop/app/
    core/audio/
    core/settings/
    core/voice/
    feature/dashboard/
    feature/revealwho/
    feature/voicepoc/
    feature/wordchain/
    game/revealwho/
    game/wordchain/
    ui/components/
    ui/theme/
  src/main/res/
assets/
  backgrounds/
  dashboard/
docs/
  design/references/
```

- `app/src/main/res`: 앱 빌드에서 실제 사용하는 `sr_` 리소스
- `assets/backgrounds`, `assets/dashboard`: 현재 대시보드 원본·편집·재수출용 이미지(앱 실행 리소스와 분리)
- `docs/design/references`: 최신 확정 시안과 Legacy 참고 시안
- `docs/design/references/backup`: Git 제외 대상

## 문서

- `AGENTS.md`: 작업 규칙
- `docs/PRD.md`: 공모전 MVP 요구사항
- `docs/ARCHITECTURE.md`: 현재 구조와 계획 경계
- `docs/TASKS.md`: 구현·검증 작업 목록
- `docs/design/DESIGN_GUIDE.md`: 디자인과 리소스 적용 원칙

## 안전 및 개인정보

- 주행 중 사용 가능 여부는 Pleos 공식 정책 검증 전 확정하지 않습니다.
- 음성 녹음과 과거 이동·활동 기록을 불필요하게 저장하지 않습니다.
- API 키와 인증정보를 앱 코드나 Git에 포함하지 않습니다.
- 실제 차량 검증과 에뮬레이터 검증을 명확히 구분합니다.
