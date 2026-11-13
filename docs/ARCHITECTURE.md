# 상상휴게소 — 아키텍처

> 상태: 공모전 MVP의 현재 구조와 계획 경계
> 목표: 기존 단어 게임과 음성·공통 UI를 보존하면서 두 신규 게임을 독립적으로 추가한다.

## 1. 현재 기술 기준

- 단일 Android 애플리케이션 모듈 `:app`
- Kotlin, Jetpack Compose, Material 3
- applicationId / namespace / Kotlin 패키지: `com.sangsangreststop.app`
- minSdk 28, compileSdk 37, targetSdk 37
- Kotlin 2.2.10, Android Gradle Plugin 9.5.1
- Preferences DataStore
- Pleos SpeechToText / TextToSpeech SDK 3.3.1
- Android TTS 대체 구현

## 2. 실제 소스 구조

```text
app/src/main/java/com/sangsangreststop/app/
  MainActivity.kt
  SangsangRestStopApp.kt
  core/
    settings/WordChainSettingsRepository.kt
    voice/SpeechInput.kt
    voice/SpeechOutput.kt
    voice/PleosSpeechOutput.kt
  feature/
    dashboard/DashboardScreen.kt
    voicepoc/VoicePocController.kt
    voicepoc/VoicePocScreen.kt
    wordchain/
  game/
    wordchain/
  ui/
    components/
    components/popup/
    theme/
```

현재 `feature/revealwho`, `feature/soundquiz`, `game/revealwho`, `game/soundquiz` 패키지는 존재하지 않는다.

## 3. 계층과 책임

### UI

현재 구현:

- `feature/dashboard`: 브랜드, 게임 선택, 준비중 처리, 목소리 이름 표시
- `feature/wordchain`: 단어 짝짝꿍 설정·진행 화면
- `feature/voicepoc`: 음성 기능 진단 화면
- `ui/components/popup`: 공통 팝업
- `ui/theme`: 테마 토큰

계획:

- `feature/revealwho`: 짜잔! 누구? 설정·진행 화면
- `feature/soundquiz`: 쫑긋 퀴즈 설정·진행 화면
- 공통 온보딩과 목소리 선택 화면

Compose는 상태를 표시하고 사용자 이벤트를 전달한다. 게임 규칙과 정답 판정을 화면에 넣지 않는다.

### 게임 도메인

현재 구현:

- `game/wordchain`: WordChain 엔진, 한글·두음법칙·사전·힌트·음성 명령 규칙

계획:

- `game/revealwho`: 힌트 단계와 정답 판정
- `game/soundquiz`: 문제 선택, 정답 판정과 진행 상태

신규 엔진은 가능한 한 순수 Kotlin 상태 → 액션 → 새 상태 + 효과 구조로 구현한다.

### 음성·오디오

현재 구현:

- `SpeechInput`: 사용자 발화 입력 경계
- `PleosSpeechInput`: Pleos STT 어댑터
- `SpeechOutput`: 음성 출력 경계
- `PleosSpeechOutput`: Pleos TTS 어댑터와 fallback 연결
- `AndroidTtsSpeechOutput`: 일반 Android TTS 대체 구현

계획:

- 쫑긋 퀴즈의 `SoundEffectPlayer` 경계
- 효과음·TTS·STT 상호 배제 상태

게임 엔진은 Pleos SDK를 직접 참조하지 않는다. ViewModel 또는 컨트롤러가 엔진 효과를 음성·오디오 구현으로 전달한다.

### 저장소

현재 `WordChainSettingsRepository`는 난이도와 힌트 설정을 DataStore에 저장한다.

향후 최소 저장 대상:

- 게임별 `onboardingCompleted`
- 선택한 목소리
- 필요한 게임 설정

저장하지 않는 대상:

- 음성 녹음 파일
- 대화 원문
- 위치·이동 이력
- 불필요한 게임 세션 기록

## 4. 현재 내비게이션

`SangsangRestStopApp`의 Compose 상태 전환이 다음 목적지를 관리한다.

- Dashboard
- WordChainSetup
- WordChainPlay
- VoicePoc

단어 짝짝꿍 카드는 설정 화면으로 이동한다. 짜잔! 누구?와 쫑긋 퀴즈 카드는 준비중 팝업을 표시한다. 공통 Navigation 라이브러리 기반 그래프는 현재 사용하지 않는다.

## 5. 단어 짝짝꿍 구조

현재 구성:

- `WordChainGame`: 규칙과 상태 전환
- `WordChainRules`, `Hangul`, `WordLexicon`, `HintProvider`
- `WordChainViewModel`: 저장·음성·화면 상태 연결
- `WordChainSetupScreen`, `WordChainPlayScreen`
- `WordChainSettingsRepository`
- 엔진·규칙 단위 테스트

내부 WordChain 명칭은 유지한다. 사용자 표시 문자열만 단어 짝짝꿍으로 통일한다.

## 6. 계획된 신규 게임 구조

### 짜잔! 누구?

아직 미구현이다. 예상 구성은 다음과 같으며 구현 전 세부 데이터 정책을 확정한다.

- RevealWhoQuestion: 정답, 카테고리, 힌트 목록, 동의어
- RevealWhoState / RevealWhoAction / RevealWhoEffect
- RevealWhoViewModel과 Compose 화면
- 로컬 문제 데이터와 순수 Kotlin 테스트

기존 스무고개 엔진을 재사용하는 구조가 아니다.

### 쫑긋 퀴즈

아직 미구현이다. 예상 구성:

- SoundQuizQuestion: 정답, 카테고리, 음원 리소스, 동의어, 힌트
- SoundQuizState / SoundQuizAction / SoundQuizEffect
- SoundEffectPlayer, ViewModel과 Compose 화면
- 라이선스가 확인된 로컬 음원

재생 중에는 STT를 중지하고 재생 완료 후 듣기를 시작한다.

## 7. 공통 온보딩 설계 계획

다음 이름과 구조는 **설계 제안**이며 현재 구현된 클래스나 인터페이스가 아니다.

### 상태

- `GameId`: WORD_PAIR, REVEAL_WHO, SOUND_QUIZ
- `OnboardingStep`: RULE_OFFER, RULE_EXPLANATION, PRACTICE_OFFER, PRACTICE, READY_TO_PLAY
- `OnboardingSessionState`: 현재 게임, 단계, 질문 시도 횟수, TTS/STT 상태, 확정 입력 여부, 중단 사유
- 게임별 영구 값 `onboardingCompleted`

규칙 설명 선택과 연습 선택은 각각 독립된 단계로 모델링한다. 현재 세션 상태와 영구 완료 상태를 분리하고, 본게임 진입이 확정된 뒤에만 완료 값을 저장한다.

### 액션

- 안내 TTS 완료
- 음성 인식 결과 수신
- ‘응’·‘아니’ 터치
- 인식 실패·무응답·재시도
- 규칙 설명 완료
- 연습 1회 완료
- 전화·내비·앱 백그라운드 중단과 복귀
- 게임 선택 화면으로 종료

긍정·부정이 불확실한 결과는 선택 액션으로 변환하지 않는다. 각 선택 단계는 확정 여부 또는 이벤트 토큰을 사용해 음성·터치 동시 입력을 한 번만 처리해야 한다.

### 효과

- `Speak`, `StartListening`, `StopListening`
- 규칙 설명 재생
- 연습 1회 시작
- 화면 버튼 안내
- 완료 상태 저장
- 본게임 시작
- 게임 선택 화면 복귀

TTS 완료 콜백 뒤에 STT 시작 효과를 발생시키고, TTS 중에는 STT를 중지한다. 첫 인식 실패에는 재안내, 두 번째 실패에는 터치 버튼 안내 효과를 낸다.

### 저장

Preferences DataStore에 게임별 완료 여부를 저장하는 별도 저장 경계를 추가하는 방향을 검토한다. 현재 `WordChainSettingsRepository`는 난이도와 힌트만 저장하므로 온보딩 저장을 담당하지 않는다. 중단된 세션은 완료로 저장하지 않는다.

### 기존 코드와 연결

기존 WordChain 엔진은 재작성하지 않고 상위 온보딩 흐름 또는 어댑터가 본게임 시작을 호출한다. 현재 `VoiceCommandClassifier`에는 질문 대기 중 Yes/No 분류가 있고 `WordChainViewModel`에는 TTS 완료 후 STT를 시작하는 흐름이 있으나, 이는 공통 온보딩 구현이 아니다. 신규 정책의 제한된 기본 표현, 실패 단계, 터치 입력, 중복 방지와 게임별 저장을 별도 테스트해야 한다.

전화·내비 중단 시 세션 단계 보존을 목표로 하지만 Pleos 이벤트 자동 중단·재개는 공식 지원과 실제 환경을 확인하기 전 완료로 표시하지 않는다.

## 8. 공통 팝업

현재 `CommonPopupType`, `CommonPopupUiModel`, `CommonPopupOverlay`와 `SangsangRestStopPopupOverlay`가 존재한다.

지원 UI:

- 전화
- 내비게이션
- 음성 인식·네트워크 오류
- 사용자 일시정지
- 게임 종료
- 준비중 안내

공통 팝업 UI와 Preview는 구현되어 있으나 전체 게임 런타임과 차량 이벤트에는 완전히 연결되지 않았다. Debug 소스에는 팝업 갤러리 화면이 있다.

## 9. Pleos 경계

확인된 코드·설정:

- Pleos SDK Maven 저장소
- STT/TTS 3.3.1 의존성
- `pleos.car.permission.STT_SERVICE`, `TTS_SERVICE`
- Pleos 음성 어댑터
- 단어 짝짝꿍의 TTS → STT 흐름

검증 필요:

- 새 applicationId 등록·허용 목록 또는 서명 조건
- Pleos 환경 STT 실제 인식 정확도와 무응답 원인
- TTS 실제 출력 품질
- 전화·내비·시동·오디오 포커스 API
- 주행 중 게임과 화면 입력 허용 범위
- SDK와 compileSdk/targetSdk 37 호환성
- 실제 차량과 에뮬레이터 차이

Pleos 기능은 공식 확인과 환경 검증 전 완료로 표시하지 않는다.

## 10. 리소스 정책

현재 실제 경로:

- `app/src/main/res/drawable-nodpi`: 런처 원본, 대시보드, 단어 게임과 팝업의 활성 `sr_` PNG
- `app/src/main/res/drawable*`: 런처 아이콘 XML
- `assets/backgrounds`: 대시보드 배경 원본·편집용 이미지
- `assets/dashboard`: 대시보드 카드 원본·편집용 이미지
- `assets/games/imagination`, `assets/games/twenty`: `.gitkeep`만 남은 빈 Legacy 자리표시자 디렉터리
- `docs/design/references`: 최신 확정 시안과 Legacy 참고 시안
- `docs/design/references/backup`: Git 제외 대상

실행 경로와 루트 `assets`에 있던 미사용 `mt_` 파일 79개는 참조 조사, 프로젝트 외부 백업, SHA-256 검증, 빌드·테스트 후 삭제됐다. 활성 리소스는 `sr_` 이름을 사용한다.

과거 시안은 실행 리소스가 아니며 사용자 승인 없이 이동·삭제하지 않는다. 새 리소스는 기존 파일을 무단으로 덮어쓰지 않고 출처·용도·라이선스를 확인한다.

## 11. 테스트와 검증

현재 단위 테스트 경로:

- `core/settings/WordChainSettingsRepositoryTest.kt`
- `game/wordchain/VoiceCommandClassifierTest.kt`
- `game/wordchain/WordChainGameTest.kt`
- `game/wordchain/WordChainRulesTest.kt`
- 기본 `ExampleUnitTest.kt`

2026-11-13 기준선:

- Debug clean build 성공
- 단위 테스트 47개 통과
- Git 포함 후보 194개만으로 만든 깨끗한 사본 build·test 성공
- 주요 Compose Preview 렌더링 확인
- Pleos 에뮬레이터에서 대시보드와 단어 짝짝꿍 설정·진행 화면 확인

남은 테스트:

- ViewModel의 음성 효과 순서와 중단 복구
- 실제 STT 인식·TTS 출력 품질
- 공통 팝업 런타임 연결
- TTS → STT 및 향후 음원 → STT 순서
- 권한 거부·무응답·오디오 포커스 복구
- 실제 차량 검증

## 12. 주요 위험

- 패키지 이전 후 Pleos 앱 등록·허용 목록과의 호환성
- Pleos 앱 등록이 기존 식별자·서명에 묶여 있을 가능성
- 두 신규 게임의 데이터·음원 라이선스 미확정
- TTS·효과음·STT 동시 사용 충돌
- 차량 이벤트와 주행 정책 미검증
- 공통 온보딩·종료 흐름 미구현
