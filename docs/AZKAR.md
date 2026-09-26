# Azkar content

The adhkar are the product. They are transcribed content, not generated content: nothing in this
repository may rewrite, normalise, reflow, shorten, paraphrase or "improve" them.

## Mandatory sources

The Arabic text, the order, the numbering and the **per-item repetition counts** come from:

| Category | Items | Source |
|---|---|---|
| أذكار الصباح | 31 | https://www.islambook.com/azkar/1/أذكار-الصباح |
| أذكار المساء | 30 | https://www.islambook.com/azkar/2/أذكار-المساء |
| التسبيح | 17 | https://www.islamiokul.com/arabic/zikirler.html |

The URLs live in `assets/azkar.json` under `sources` and travel with every row into the database
(`sourceReference`, plus a mechanically derived `sourceName` = host), so the reader can always show
the real attribution under "المصدر".

Repetition counts as bundled:

```
morning  1,3,3,3,1,1,3,4,1,7,3,1,1,3,3,3,1,3,1,1,3,10,3,3,3,3,1,1,100,100,100
evening  1,1,3,3,3,1,1,3,4,1,7,3,1,1,3,3,3,1,3,1,1,3,10,3,3,3,3,100,1,100
tasbeeh  100 × 17
```

Evening is deliberately **not** assumed to mirror morning: it has the extra "آمن الرسول" item and a
different tail order.

## File format

```jsonc
{
  "sources": { "morning": "…", "evening": "…", "tasbeeh": "…" },
  "morning": [
    { "id": 1, "order": 1, "text": "…", "repeat": 1, "virtue": "…" }
  ],
  "evening": [ … ],
  "tasbeeh": [ … ]
}
```

## How it reaches the screen

```
assets/azkar.json ──AzkarSeedSource.load()──► List<DhikrEntity> ──DAO insert(IGNORE)──► Room
                                                                        │
                                            DhikrRepository.observeCategory(category)
                                                                        │
                                                ReadingViewModel ──► ReadingScreen
```

* `AzkarSeedSource` copies `text`, `repeat`, `order` and `virtue` **verbatim**. It does not trim,
  normalise, truncate or re-encode anything; an item without text is skipped rather than replaced by
  invented content. The only derived values are `seedKey` (`"morning:1"`), `sourceName` (the host of
  the recorded URL) and `includeInOverlay` (`text.length <= 180`) - a display decision about which
  verified text fits a small card, never an edit of it.
* Seeding is idempotent twice over: it runs only when the stored seed version is older than
  `AzkarSeedSource.SEED_VERSION`, and `seedKey` carries a unique index with
  `OnConflictStrategy.IGNORE`, so a second run cannot duplicate a row.
* Seeded rows are immutable content: `DhikrRepository.save()` only lets the user flip their
  `isEnabled` / `includeInOverlay` switches, and the DAO delete is
  `DELETE FROM dhikr WHERE id = :id AND isDefault = 0` - a default dhikr cannot be deleted.
* The user's own dhikr (`DhikrCategory.CUSTOM`) live in the same table with `isDefault = 0` and are
  fully editable.

## Reading behaviour

* Each item shows the verified text at the reader's chosen size (0.9-1.4), its repetition counter,
  its virtue and its source; the text itself is never reflowed or re-styled.
* Counting down to zero marks the item complete, the header progress advances, and the list scrolls
  to the next item.
* Progress is per session: reopening the screen restarts every counter from its own target.

## Guardrails

`tests/test_content_integrity.py` fails if any of the following changes:

* the SHA-256 of `assets/azkar.json`, or its difference from the committed blob,
* the section counts 31 / 30 / 17, the uniqueness of `id` / `order`, or an empty text,
* the presence of a source for every section,
* the verbatim seeding path (`arabicText = text`, no `trim`, `replace`, `normalize`, `substring`,
  `take`, `filter` on the text),
* the "skip, never invent" rule for an empty item,
* the immutability guarantees (unique `seedKey`, `IGNORE` conflict strategy, guarded delete).
