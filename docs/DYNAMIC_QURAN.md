# Dynamic Online Quran Reader Integration

The Quran reader feature dynamically fetches Surahs and Ayahs online via the official Alquran Cloud API (`https://api.alquran.cloud/v1/surah/{surah_number}/quran-uthmani`). It replaces all local database / asset dependencies with dynamic fetching, tiered caching, clean loading states, and error retry handling.

## Architecture

1. **Alquran Cloud API**:
   - Endpoint: `https://api.alquran.cloud/v1/surah/{surah_number}/quran-uthmani`
   - Response format: JSON containing Surah metadata and Uthmani verse texts (`ayahs` array with `numberInSurah`, `text`, `juz`, `page`).

2. **Tiered Caching**:
   - **In-Memory Cache**: Fast memory cache (`ConcurrentHashMap` in Android / `Map` in JS) avoids refetching loaded Surahs during the active session.
   - **Persistent Cache**: Disk cache in the application cache directory (`quran_cache/surah_{number}.json`) / `localStorage` allows instant offline display once a Surah has been fetched.

3. **UI States & Error Handling**:
   - **Loading State**: Displays a clean loading spinner while the network request is in flight.
   - **Error State & Retry**: On network interruption or timeout, displays an error message along with an interactive "Retry" button. Tapping Retry triggers an immediate refetch.
   - **Content State**: Reflowable Arabic text with interactive verse selection highlight (`#D0ECE4`), updating active Juz and Page counters.

4. **Typography & Styling**:
   - Standard Arabic web fonts via Google Fonts CDN (`Amiri` and `Scheherazade New`).
   - Clean color palette matching the app theme: `#FBF9F0` (paper background), `#1A1A1A` (ink), `#7A7A7A` (muted), `#2D7D46` (primary green).

## Implementation Files

| File | Description |
| --- | --- |
| `app/src/main/java/com/clock/livewallpaper/quran/QuranApiClient.java` | Dynamic API client with network fetching, JSON parsing, in-memory & disk caching |
| `app/src/main/java/com/clock/livewallpaper/activity/QuranActivity.java` | Reader Activity with loading state, error retry button, clickable verses, and native edge-to-edge system bars |
| `app/src/main/res/layout/activity_quran.xml` | Responsive RTL layout with progress bar, status text, and retry button |
| `app/src/main/res/values/quran.xml` | Reader colors, strings, and theme definitions |
| `web/index.html` | Standalone interactive web component with Google Fonts CDN, localStorage caching, loading spinner, and retry button |
| `app/src/main/assets/quran/index.html` | Bundled web reader asset |
| `tests/test_quran_api.py` | Unit tests for API contracts, caching logic, layout wiring, and web component specifications |

## Verification

Run all test suites:

```sh
python3 -m unittest discover tests
```
