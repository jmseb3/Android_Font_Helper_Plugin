# Changelog

## [Unreleased]

## [2.2.0]
- Improved: Generated Kotlin identifiers now preserve readable camel case from the entered class name.
- Improved: Generated font resource names now use `snake_case` for clearer Android and Compose resource references.
- Improved: Main screen UI was split into focused section/support files to keep the tool window flow easier to maintain.
- Improved: ZIP and Google Fonts import branching was moved out of the main screen into import support utilities for more predictable behavior.

## [2.1.3]
- Fixed: Restored Compose desktop runtime packaging for all target platforms to prevent missing Skiko native library errors.

## [2.1.2]
- Improved: Plugin package size reduced significantly by trimming unnecessary bundled runtimes.
- Improved: UI icon consistency restored while keeping a lightweight core icon set.
- Improved: Action icons diversified for better visual hierarchy in import and dialog flows.

## [2.1.1]
- New: Setup/Fonts tab layout to reduce scrolling and improve workflow focus.
- New: Module list reload automatically when Gradle sync/import is finished.
- New: Google Fonts first dialog now includes "Auto update Font Class Name" option (default enabled).
- Improved: Font table responsive layout and compact threshold for narrower tool windows.
- Fixed: Runtime classloader conflicts after Generate by excluding conflicting runtime libs and opening files safely.
