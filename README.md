# 어항닥터 (TankDoctor)

어항 사진을 찍으면 AI(Google Gemini)가 어항 상태를 진단하고 해결 방법을 알려주는 안드로이드 앱.

## 무엇을 진단하나요

- 물 상태: 백탁, 녹수, 유막, 수위
- 이끼(조류): 녹점, 규조류, 사상조, 흑사조, 남조류
- 물고기: 백점병, 지느러미 녹음, 곰팡이, 복부 팽창, 수면 호흡, 과밀
- 수초: 누렇게 변함, 녹아내림, 영양 결핍
- 바닥재·장비: 찌꺼기, 먹이 잔여물, 여과기·히터 상태

결과: 건강 점수(0~100), 발견된 문제별 근거·원인·해결책, 오늘 할 일 순서, 사진으로 알 수 없는 수질 검사 권장.

## 설치

1. 휴대폰에서 **https://tinyurl.com/tankdoctor** 를 열면 최신 APK가 바로 받아집니다. ([Releases](../../releases/latest)에서 받아도 됩니다.)
2. 설치 시 "출처를 알 수 없는 앱" 허용을 켭니다.
3. 앱 실행 → 설정 → [Google AI Studio](https://aistudio.google.com/apikey)에서 받은 무료 API 키 입력.

Android 8.0 이상 지원.

## 개인정보

- 진단할 때 사진(약 1536px로 축소)과 메모가 Google Gemini API로 전송됩니다.
- API 키는 휴대폰 내부 저장소에만 보관되며 백업되지 않습니다.
- 앱 자체 서버는 없습니다.

## 빌드

GitHub Actions가 `main` 푸시마다 APK를 빌드하고, `v*` 태그를 푸시하면 Release를 만듭니다.

```bash
git tag v1.0.1 && git push origin v1.0.1
```

로컬 빌드는 Android SDK + Gradle 8.11 필요: `gradle assembleDebug`.

서명 키는 저장소 시크릿(`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`)에 있습니다.
