RIFT PROTOCOL - ORIGINAL AUDIO BANK 1.9.1

144 deterministic WAV samples, authored procedurally for RIFT Protocol.
No Counter-Strike samples, recordings, downloads or converted game assets.
These sounds belong to the RIFT project and follow its distribution terms.

Format: PCM signed 16-bit, mono, little-endian, 22050 Hz.
Three variants per event; filenames replace the runtime '#' with '-'.

45 shots: 15 RIFT weapons, with transient, pressure body, mechanism and tail.
27 surface events: step, land and impact on concrete, wood and metal.
72 mechanisms: reload, magout, magin and bolt for six weapon categories.

Reload is a short 0.12-second onset cue, not a complete mechanical sequence.
Magout, magin and bolt are separate samples scheduled by weapon reload progress.
This avoids duplicated stages and keeps timing aligned with each reload duration.

The generator uses seeded noise, filtered contact, damped resonators and
delayed mechanical layers. Samples have DC correction, short endpoint fades
and a peak ceiling of 0.94. Different variants change pitch and seeded noise.

Regenerate from the project directory after compiling:
java -cp build/classes rift.AudioAssets assets/audio

Sources: src/rift/AudioAssets.java and src/rift/ShotAudio.java.
The runtime uses packaged WAVs, with the same synthesis as an offline fallback.
SHA256SUMS.txt lists the reproducible checksum of every WAV file.
