# 앨범 태그

Android 사진 저장소의 앨범을 읽어 앨범 단위로 여러 태그를 붙이고 분류하는 앱입니다. 원본 사진과 갤러리 앨범 구성은 변경하지 않습니다.

## 주요 기능

- 기기의 사진 앨범과 썸네일 조회
- 사진과 동영상을 함께 조회하고 동영상 전체 화면 재생
- 앨범 내부 사진 그리드 및 전체 화면 보기
- 전체 화면 좌우 이동, 확대·축소, 촬영일·사진 번호 표시
- 앨범별 복수 태그 추가·삭제
- 태그 이름 변경 및 삭제
- 여러 앨범을 선택해 태그 일괄 추가
- 앨범을 길게 눌러 다중 선택 시작
- 최신순·오래된순·이름순·미디어 수순·사용자 지정 앨범 정렬
- 앨범 내부 미디어 다중 선택, 공유 및 다른 앱으로 열기
- 선택한 사진·동영상의 다른 앨범 복사·이동 및 영구 삭제
- 대표 사진, 미디어 수, 촬영 기간, 저장 위치, 최근 추가일 앨범 정보
- 태그를 학창시절, 취미, 여행, 함께한 사람 같은 그룹으로 구성
- 태그 AND/OR 검색 및 미분류 앨범 필터
- 태그 데이터 JSON 백업·복원

## DDD 구조

```text
app/src/main/java/com/albumtags/app/
├── domain/
│   ├── model/          순수 Kotlin 도메인 모델
│   ├── repository/     저장소 인터페이스
│   └── usecase/        태그 및 앨범 업무 규칙
├── data/               MediaStore와 앱 내부 저장소 구현
├── presentation/       Compose UI, 화면 상태, ViewModel
├── di/AppContainer.kt
└── AlbumTagsApplication.kt
```

의존성은 `presentation → domain ← data` 방향입니다. 도메인 계층은 Android 프레임워크를 참조하지 않으며 data 계층은 도메인의 저장소 인터페이스를 구현합니다. `AppContainer`가 구현체와 유스케이스를 조립합니다.

## 빌드

Android Studio에서 이 폴더를 열고 Gradle 동기화 후 실행하거나 다음 명령을 사용합니다.

```powershell
.\gradlew.bat assembleDebug
```

APK는 `app\build\outputs\apk\debug\app-debug.apk`에 생성됩니다. Galaxy에서 처음 실행할 때 사진 접근 권한을 허용해야 합니다.

## 데이터 주의사항

- 태그와 태그 그룹은 앱 내부 저장소에 저장됩니다.
- 앱을 삭제하면 데이터도 삭제되므로 먼저 JSON 백업을 만들어 두세요.
- 사진 파일, EXIF 정보, 삼성 갤러리의 앨범은 수정하지 않습니다.
