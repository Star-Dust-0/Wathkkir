#!/bin/sh
# Pure-Java check against the exact application calculator. No Android/UI substitutes.
set -eu
OUT=build/location-fixture
mkdir -p "$OUT"
javac -encoding UTF-8 -d "$OUT" app/src/main/java/com/example/fajr/calculation/*.java tools/parity/PrayerLocationFixture.java
java -cp "$OUT" PrayerLocationFixture 30.8619 29.5845 '04:46 06:14 11:45 14:52 17:16 18:35'
java -cp "$OUT" PrayerLocationFixture 30.0444 31.2357 '04:39 06:07 11:39 14:47 17:10 18:29'
