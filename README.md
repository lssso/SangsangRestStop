# 상상휴게소 (SangsangRestStop)

차 안에서 잠깐 쉬어 가듯, 듣고 말하며 즐기는 어린이·가족용 음성 놀이 앱입니다.

> 대상: Hyundai Pleos Connect Android 앱 공모전
> 현재 상태: 독립 저장소 리브랜딩과 실행 리소스 정리 완료. 단어 짝짝꿍은 실행 가능하며, 짜잔! 누구?와 쫑긋 퀴즈는 준비중입니다.

상상휴게소는 별도 Git 이력과 원격 저장소를 사용하는 독립 프로젝트입니다. 분리 원본 프로젝트의 코드와 리소스는 이 저장소에서 변경하지 않습니다.

## 공모전 MVP

1. **단어 짝짝꿍**
   - 기존 WordChain 엔진과 설정·진행 화면 재사용
   - 마지막 글자로 단어를 이어 말하는 놀이
2. **짜잔! 누구?**
   - 앱이 힌트를 순차적으로 제공
   - 사용자가 정답을 음성으로 맞히는 게임
3. **쫑긋 퀴즈**
   - 동물·탈것·악기 소리를 듣고 정답을 맞히는 게임

현재 MVP에 포함되지 않는 이전 기획 시안은 `docs/design/references`의 Legacy 참고 자료로만 보존합니다.

## 현재 구현 상태

### 구현 및 현재 저장소 검증 완료

- 상상휴게소 프로젝트명, 앱 이름, 런처 아이콘과 패키지 식별 정보
- `applicationId`, Android namespace, Kotlin 패키지: `com.sangsangreststop.app`
- Jetpack Compose 대시보드와 MVP 게임 카드 3종
- 상상휴게소 로고, 캐릭터 포함 배경, 카드 배경·아트워크의 `sr_` 앱 리소스
- 단어 짝짝꿍 설정·진행 화면
- 순수 Kotlin WordChain 엔진과 단위 테스트
- 난이도·힌트 설정 DataStore 저장
- Pleos SpeechToText / TextToSpeech SDK 어댑터 코드
- Pleos TTS 실패 시 Android TTS 대체 구현과 STT 불가 시 직접 입력 경로
- 공통 팝업 UI와 Compose Preview
- 음성 기능 진단 화면
- 앱 백그라운드 전환 시 단어 게임 일시정지·재개
- 대시보드의 현재 목소리 표시 이름 `상상놀이친구`

### 부분 구현 또는 별도 검증 필요

- Pleos 에뮬레이터에서 앱 설치·대시보드·단어 짝짝꿍 설정·진행 화면 실행 확인
- 음성 상태의 말하기 → 듣기 전환 확인
- Pleos STT 실제 인식 정확도와 무응답 원인
- TTS 실제 출력 품질
- 공통 팝업의 전체 런타임 게임 흐름 연결
- 목소리 선택·저장 화면: 명칭만 확정됐으며 기능은 미구현
- 전화·내비·시동·오디오 포커스 차량 이벤트
- 주행 중 사용 범위와 UI 제한

### 미구현

- 짜잔! 누구? 엔진·화면·문제 데이터
- 쫑긋 퀴즈 엔진·화면·문제 데이터·라이선스가 확인된 음원
- 게임별 음성 온보딩과 완료 상태 저장
- 신규 게임 설정·진행 화면 전용 이미지

## 확정된 온보딩 목표

각 게임의 최초 실행은 다음 흐름을 목표로 합니다.

1. 상상놀이친구가 규칙 설명 여부를 묻고 음성 또는 큰 ‘응’·‘아니’ 버튼으로 선택
2. 규칙 설명 완료 또는 생략 후 연습 1회 여부를 별도로 선택
3. 연습 완료 또는 생략 후 본게임 시작

이후 실행은 기본적으로 본게임으로 바로 진입하되 규칙 다시 듣기와 다시 연습하기를 제공합니다. 인식 실패나 무응답을 ‘아니’로 간주하거나 단계를 자동 생략하지 않습니다.

이 온보딩 UI·상태·저장 기능은 **현재 미구현**입니다. WordChain의 기존 음성 명령 분류와 TTS→STT 처리 일부는 구현 참고 대상일 뿐, 공통 온보딩 구현 완료를 의미하지 않습니다.

## 최근 기준선 검증

2026-11-13 기준 작업 기록과 현재 파일을 대조했습니다.

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

## 실행

1. Android Studio에서 저장소 루트를 엽니다.
2. Gradle Sync를 수행합니다.
3. 일반 Android 에뮬레이터 또는 공식 Pleos 테스트 환경을 선택합니다.
4. `app` 실행 구성을 실행합니다.

일반 Android, Pleos 에뮬레이터, 실제 차량의 검증 결과를 혼용하지 않습니다. 실제 차량 검증은 아직 완료되지 않았습니다.

## 저장소 구조

```text
app/
  src/main/java/com/sangsangreststop/app/
    core/settings/
    core/voice/
    feature/dashboard/
    feature/voicepoc/
    feature/wordchain/
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
