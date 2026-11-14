# Asko

A personal workout logger for Android phones and tablets, built with Jetpack Compose and
Material 3 Expressive (in the style of Google Health). Each exercise comes with images and
animations that show the correct form.

## Stack

| Area | Choice |
|---|---|
| UI | Jetpack Compose, Material 3 Expressive (`material3` 1.5 alpha), Material You dynamic color |
| Font | Google Sans Flex, variable (SIL OFL; `res/font`, license in `assets/licenses`) |
| Adaptive layout | `NavigationSuiteScaffold`: short navigation bar on phones, wide navigation rail on tablets |
| Navigation | Navigation 3 (type-safe `@Serializable` routes) |
| Storage | Room (workout logs), JSON file synced from ExerciseDB (exercise library) |
| Exercise visuals | Coil + coil-gif (ExerciseDB's animated demos) |
| DI | Manual: one `AppContainer` |

Min SDK 31 (Android 12), so dynamic color is always available.

## Project structure

```
app/src/main/
├── kotlin/io/github/dimitrysaf/asko/
│   ├── AskoApplication.kt      App entry point; builds the AppContainer
│   ├── MainActivity.kt         Single activity hosting Compose
│   │
│   ├── pages/                  Full screens, one folder per feature (Page + its ViewModel)
│   │   ├── routines/           Week streak, routines list, routine page, editor + exercise picker
│   │   ├── discovery/          Search and filter the exercise library
│   │   ├── exercises/          Exercise detail (form visuals, description, videos)
│   │   ├── workout/            Active workout logging
│   │   └── history/
│   │
│   ├── components/             Reusable UI pieces shared across pages
│   │                           (PageScaffold, ExerciseGallery, ExerciseThumbnail, ExerciseListItem, …)
│   │
│   ├── data/
│   │   ├── model/              Plain data classes the UI works with (Exercise, Workout, …)
│   │   ├── exercisedb/         Downloads exercises from ExerciseDB's free API
│   │   ├── catalog/            Keeps the exercise library saved on the device and refreshes it
│   │   ├── local/              Room database, entities, DAOs, entity → model mappers
│   │   └── repository/         The only entry point pages use to read/write data
│   │
│   ├── navigation/             Routes, tabs and the app shell (AskoNavHost)
│   ├── theme/                  Material Expressive theme: colors, type, shapes, motion
│   ├── constants/              Frequently used values (spacing, asset paths)
│   └── lib/                    Helpers with no UI of their own
│       ├── di/                 AppContainer + appViewModel { } helper
│       ├── format/             Date, duration and weight formatting
│       └── time/               Calendar helpers
│
└── res/                        Strings, launcher icon, window theme
```

Rules of thumb:

- A **page** owns a screen and its ViewModel. Pages never talk to Room directly, only to repositories.
- A **component** takes plain data and callbacks; it has no ViewModel. If two pages need it, it belongs here.
- **data/model** is what the UI sees; **data/local/entity** is how it's stored. Mappers convert between them.
- User-facing text lives in `res/values/strings.xml`.

## Exercise library (ExerciseDB)

Exercises come from [ExerciseDB](https://oss.exercisedb.dev)'s free API
(`data/exercisedb/ExerciseDbApi.kt`, no account or key): 1,500 exercises, each with an animated
demo, step-by-step instructions, target muscles, body part and equipment. On first use the app
downloads the whole library (60 pages of 25) into `exercisedb.json` in its private files, so it
works offline. It downloads again once that copy is a week old, and keeps the old copy if the
phone is offline.

- **Clean-up while syncing:** exercises whose GIF is missing on ExerciseDB's server are dropped
  (checked with a light `HEAD` request each), and duplicates keep one entry per name.
- **Animations** are GIFs, played by Coil's GIF module and cached on the device. Lists show only
  their first frame, so a long list doesn't animate dozens of GIFs at once.
- **Size:** the free GIFs are 180 × 180 px, so the exercise page caps the demo at 480 dp wide.
- **Rights:** ExerciseDB doesn't state who may reuse the animations. Fine for a private app on
  your own phone; before publishing Asko, get ExerciseDB's paid license or another source.

## Building

Open the project in Android Studio, or from a terminal (using Android Studio's bundled JDK):

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:installDebug
```

`gradle.properties` caps Gradle at 4 workers and low CPU priority so builds don't freeze the desktop.

This codebase was created by AI. 
Some elements of the UI is handmade. 
