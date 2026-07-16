# Ekte Apps

Free, honest Android apps — no ads, no tracking, no accounts, no nonsense. Just small tools that do
their job and leave you alone, all under the **“Ekte &lt;Name&gt;”** name.

> Vibe coded with Claude Code because I'm lazy.

Each app lives in its own subdirectory as a complete, standalone Gradle project.

## Apps

| Directory | Name | Application id | What it does |
|-----------|------|----------------|--------------|
| [`steps/`](steps/) | **Ekte Steps** | `com.ektebrysjan.steps` | Pedometer — today's steps, the previous 6 days, and simple stats over your local history. Counts in the background, no notification. |
| [`notes/`](notes/) | **Ekte Notes** | `com.ektebrysjan.notes` | Notes — write, tag, search, optional PIN-locked private notes, and manual export/import. Zero permissions, fully offline. |
| [`todo/`](todo/) | **Ekte Todo** | `com.ektebrysjan.todo` | To-do — multiple lists, tasks with optional due date and note, mark done, archive/delete, and CSV export/import. Zero permissions, fully offline. |
| [`workout/`](workout/) | **Ekte Workout** | `com.ektebrysjan.workout` | Workout log — log type, duration, intensity and notes; weekly/monthly summaries; filter by type; CSV export/import. Zero permissions, fully offline. |

## The charter

Every app in the suite sticks to these rules:

1. **App id** is `com.ektebrysjan.<shortname>` (Ekte Steps → `com.ektebrysjan.steps`).
2. **100% free** — no ads, no tracking, no analytics.
3. **Local-only** — no cloud sync, no accounts.
4. **Privacy-first** — minimal permissions; ideally **no `INTERNET`** at all, so the app physically
   can't phone home.
5. **Dark by default** — one clean, minimalist dark theme. No Material You / dynamic colour.
6. **Open assets only** — Material Icons and other open resources.
7. **No dark patterns** — no gamification, no engagement or retention tricks.
8. **No gratuitous notifications** — only when they're genuinely core to the app (e.g. a calendar's
   reminders). Silent background work otherwise.
9. **Consistent header** — the top bar shows the short app name, with *“an Ekte App by EkteBrysjan”*
   beneath it.
10. **Settings screen** — every app has one with a **Backup** section (manual export/import to a
    local file) and an **About** section (app name, the credit line, version, and a link to
    github.com/ektebrysjan). No separate in-app "Privacy" section.

Roadmap: utility and lifestyle apps first (steps, notes, todo, habits, trackers), technical tools
later (WiFi scanner, decibel meter, and friends).
