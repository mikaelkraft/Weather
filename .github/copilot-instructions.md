## Quick context

- This repository is a small Java "Weather" application (see `README.md`). At present the workspace only contains the top-level README; there were no detected build files (no `pom.xml`/`build.gradle`) or `src/` tree when these instructions were generated.

## Goals for an AI coding agent

- Help locate the project's runtime entry point (e.g., classes with `public static void main`) and the primary package under `src/main/java` if present.
- Discover the build system (Maven/Gradle) by looking for `pom.xml` or `build.gradle` at repo root and run corresponding build/test commands.
- If build files are absent, ask the human for the preferred build/run commands before making changes that require compilation or CI changes.

## Where to start (concrete steps)

1. Inspect `README.md` (already present) for basic project description and any run/build hints.
2. Search the repo for common Java layouts: `src/main/java`, `src/test/java`, `src/main/resources`.
3. Look for `pom.xml` or `build.gradle` in the repo root to determine Maven vs Gradle; prefer the found tool and do not assume one if neither exists.
4. Find the main class by searching for `public static void main` or classes named `Main`, `App`, or `Application`.
5. For tests, search for `src/test/java` or files annotated with `@Test` and run the build tool's test task (see 'Commands' below).

## Commands (use only when corresponding files are detected)

- If `pom.xml` present: `mvn -q -DskipTests=false test` to run tests and `mvn -q package` to build.
- If `build.gradle` present: `./gradlew test` (or `gradle test` if wrapper absent) and `./gradlew build`.
- If neither build file is present: do NOT invent a command — ask the repo owner or open an issue describing missing build metadata.

## Patterns & conventions to follow in this repo

- Rely on standard Java layout assumptions if a `src/` tree exists: `src/main/java` for production code, `src/test/java` for tests, and `src/main/resources` for config.
- When searching for the app entry point, prefer explicit `main` methods over guesses. Example search pattern: `grep -R "public static void main" -- src || git grep -n "public static void main"`.
- Prefer non-invasive changes (small focused commits) and include a short test or compilation check when modifying core code.

## When to ask for human input

- If no build files or src tree are present. Mention what you searched for and what you couldn't find.
- If multiple build systems or multiple possible main classes exist: list candidates and ask which is the intended entry point.

## Examples of actionable tasks for the agent

- "Find and run the project's tests": locate `pom.xml` or `build.gradle`; run tests; report failures and stack traces.
- "Add a unit test for X": locate the package/class, follow existing test naming conventions (if any), create a test under `src/test/java` parallel to the class package.
- "Locate application entry point": search for `public static void main` and report file path(s) with a one-line description of what each main does.

## Contract (inputs / outputs / success criteria)

- Input: repository root. Output: paths for build files, main class candidate(s), and test locations. Success: can run tests or build using detected tool, or clearly report missing build metadata.

## Edge cases the agent must handle

- Missing build files or source tree — do not assume; ask for clarification.
- Multiple candidates for entry point — present a short list and ask which to use.

## Files & places to reference

- `README.md` — project overview (present).
- Root — look for `pom.xml`, `build.gradle`, `gradlew`, or `gradlew.bat`.
- `src/main/java`, `src/test/java`, `src/main/resources` — conventional locations for code, tests, and config.

## Follow-up

- I created/updated this file to reflect the repository state at the time of analysis. If you want more specific instructions (for example, exact test commands, CI steps, or the preferred code style), provide the build files or a sample `src/` layout and I'll merge them into this document.

---
Please review these instructions and tell me if you want me to (a) detect and add concrete Maven/Gradle commands after you add `pom.xml`/`build.gradle`, (b) create a starter `pom.xml`/`build.gradle` for this project, or (c) scan further for additional files to improve this guidance.
