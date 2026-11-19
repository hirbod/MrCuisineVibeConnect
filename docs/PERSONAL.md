# Personal touches: `personal.json`

Toque can show a personal home screen: your own title, greetings that change with the time of day,
and a small card with a beating heart and rotating messages under the search bar. All of this comes
from one optional file **on the robot**, never from the app or this repository, so nothing private
ends up on GitHub and app updates never overwrite it.

Without the file, Toque looks as usual ("Toque", "Good morning…", no message card).

## Where the file lives

```
/sdcard/Android/data/app.toque/files/personal.json
```

(the same folder as `lang/` for extra languages). Keep the master copy on your Mac, for example in
`~/Toque/custom/personal.json`, and push it from there.

## Format

All fields are optional. UTF-8, plain JSON (no comments, no trailing commas).

```json
{
  "title": "Alex's Kitchen",
  "greetings": {
    "morning": ["Good morning, Alex", "Morning, sunshine"],
    "day":     ["Hi Alex", "Hello there"],
    "evening": ["Good evening, Alex"],
    "night":   ["Still awake?"]
  },
  "messages": [
    "The first message is always shown first",
    "Then the others rotate in random order",
    "One message every 9 seconds"
  ],
  "signature": "Optional small line under every message"
}
```

| Field | What it does |
|---|---|
| `title` | Replaces "Toque" at the top left of the home screen. |
| `greetings` | Replaces "Good morning / Good afternoon / Good evening". One is picked at random each time the home screen appears. Time slots: `morning` 05–10 h, `day` 11–17 h, `evening` 18–22 h, `night` 23–04 h. A missing or empty slot falls back to the normal greeting. With greetings set, the line is drawn in rose instead of grey. |
| `messages` | Shows the message card (bottom left of the home screen). The first entry always comes first after start, the rest rotate in random order, one every 9 s. Up to two lines are shown (about 50 characters fit comfortably). Without messages there is no card. |
| `signature` | Small grey line under each message. Leave it out if the messages already say who they are from. |

## Applying a change

From the Mac (robot reachable over ADB, see [INSTALLATION.md](INSTALLATION.md)):

```
adb push ~/Toque/custom/personal.json /sdcard/Android/data/app.toque/files/personal.json
adb shell am force-stop app.toque
adb shell am start -n app.toque/.ui.MainActivity
```

Toque reads the file when it starts, so restart it (the last two lines) or the robot. If the JSON is
invalid, Toque ignores the file and writes `personal.json ignoré : …` to *Settings › Tools › Log*;
check it with `python3 -m json.tool personal.json` before pushing.

To remove everything personal again: `adb shell rm /sdcard/Android/data/app.toque/files/personal.json`
and restart Toque.

## For AI agents

- Never put names, greetings or messages from `personal.json` into the code, commits, releases or
  issues: this repository is public. Edit the local copy and push it to the robot as above.
- The code is in `app/src/main/kotlin/app/toque/Personal.kt` (parsing), `ui/screens/HomeScreen.kt`
  (title, greeting slots) and `ui/LoveNote.kt` (message card, timings).
