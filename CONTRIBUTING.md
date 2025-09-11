# Contributing to Anesk (Arcade Pulse — Snake)

Thanks for checking out **Anesk**!  
This project is part of **Web Weavers World** and also a space to experiment with VS Code, Git, and GitHub Actions. Beginners and pros both welcome. 💙

---

## Quick Start

**Requirements**
- Java **17+** (Temurin recommended)
- Git

**Clone & build**

```bash
git clone https://github.com/LilPwinc3ss554/Anesk-v1.5.7.git
cd Anesk-v1.5.7/Anesk-structured-v1.5.2.0.2
mkdir -p bin
javac -d bin src/Anesk/*.java
```

Run (desktop)

```bash
java -cp bin Anesk.Main
```

Package JAR

```bash
mkdir -p dist build
echo "Main-Class: Anesk.Main" > build/MANIFEST.MF
jar --create --file dist/Anesk.jar --manifest build/MANIFEST.MF -C bin .
CI builds run on GitHub Actions (JDK 17). Every push compiles and uploads a JAR artifact.
```

How to Contribute
1) File an issue first (recommended)
Use Bug report or Feature request templates if available.

For small fixes, you can skip straight to a PR, but linking an issue helps.

2) Create a branch

```bash
git checkout -b feat/short-name
# or
git checkout -b fix/short-name
```

3) Follow commit conventions
- Use Conventional Commits:
 - `feat: add neon skin`
 - `fix: prevent crash on pause`
 - `docs: improve README badges`
 - `chore: update build workflow`

4) Open a Pull Request
- Target branch: main
- Keep PRs focused (smaller is better).
- Include screenshots/GIFs for UI/visual changes (skins, maps, etc.).
- Ensure it compiles on Java 17 (`javac -d bin src/Anesk/*.java`).
- CI must pass before review.

---

Project Layout

```bash
Anesk-structured-v1.5.2.0.2/
├── src/Anesk/           # Java sources (game, UI, logic)
├── assets/              # icons, sounds, skins, labyrinths
├── build/               # helper scripts & MANIFEST.MF
├── dist/                # output JAR (ignored in Git)
└── .github/workflows/   # CI (build.yml)
```

---

Style & Guidelines

## Java
- Stick to clear, readable Java 17 (use `switch` expressions where helpful).
- Prefer small methods and descriptive names.
- No heavy frameworks—Awt/Swing only (for now).
- 
## Skins
Add colors/assets under `assets/skins/<your-skin>/`.
Update any selectors or enums in `src/Anesk/Skins.java` if needed.
Include a screenshot in your PR description.

## Labyrinth maps
ASCII maps live in `assets/labs/`.
Keep dimensions consistent with existing maps.
Add a quick note about difficulty/intent.

## Sounds/Art
Only submit assets you created or that are licensed for redistribution under MIT-compatible terms.
Include attribution in the PR if required by the asset license.

---

## Tests
There aren’t formal tests yet. For now, manual checks:
- Builds on Java 17
- Start/pause/resume work
- Collision rules behave as expected
- New skins/maps load without exceptions
(If you’re keen to add lightweight tests or headless checks, open an issue to discuss approach.)

---

## Issues & Labels
- good first issue — beginner friendly
- help wanted — needs volunteer
- bug, enhancement, docs — usual suspects

If you’re new, comment on an issue and we’ll help you get started.

---

## Security
If you discover a vulnerability or exploit, do not open a public issue.
Email the maintainer (repo owner) privately or use GitHub security advisories.

---

## License
By contributing, you agree that your contributions are licensed under the
MIT License of this repository.

---

## Code of Conduct
Be kind, constructive, and respectful. We aim for a welcoming space for learners and experienced developers alike.
