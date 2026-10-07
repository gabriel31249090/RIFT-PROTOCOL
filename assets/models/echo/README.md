# ECHO Visual Pilot (1.9.2)

The fixed receiver and handguard are adapted from Quaternius' **AssaultRifle_1**
in the Ultimate Guns Pack, released under CC0. The original pack has no textures
or animations. No Counter-Strike game files were used.

- Author: Quaternius, https://quaternius.com/
- Official pack: https://quaternius.com/packs/ultimategun.html
- Official GitHub page: https://github.com/Quaternius/quaternius.github.io/blob/main/packs/ultimategun.html
- Official OBJ folder: https://drive.google.com/drive/folders/1azfiAbFOM8HFmHRIoaawr-dOJ0q0kVf-
- Original OBJ: https://drive.google.com/uc?export=download&id=1CyEzGioyY2r7SENNA5xokzeAzj67pYn_
- License: https://creativecommons.org/publicdomain/zero/1.0/
- Retrieved: 2026-10-07

`assault-rifle-source.obj` is the unmodified upstream file. `echo-body.obj` keeps
only five connected components: the upper/lower receiver, front receiver, lower
handguard and top handguard. It removes the original magazine, grip, trigger,
barrel, sights and all rail teeth. The derivative has 154 vertices and 118 faces
(280 triangles after triangulation). Coordinates are adapted to the RIFT local
axes and rifle proportions: x = -source.z * 0.70, y = (source.y - 0.52) * 0.30,
z = source.x * 0.34 + 0.08. The determinant is positive, preserving OBJ winding.
Normals and the external MTL reference were dropped. The runtime uses the proven
javagl Obj parser, baked face shading and local procedural UVs/materials.

The RIFT polymer stock, segmented curved magazine, vents, optic/sights, rail,
charging handle, muzzle, mint equipment indicators and coral chassis accents
are original additions built by `WeaponModel`. Reload magazine displacement,
firing cycle, collection rotation, skins and charms remain runtime controls.
This is a bounded static-mesh pilot, not a skinned or photorealistic PBR model.

`echo-world-body.obj` keeps only the three receiver components, with 82 vertices,
64 faces and 144 triangles. Third-person ECHO uses this reduced imported receiver
plus inexpensive procedural
equipment only at the existing detailed-actor distance. Distant ECHO keeps the
established simple weapon silhouette.

SHA-256 checksums are recorded in `SHA256SUMS.txt` beside these files.

The surface bitmap `metal_plate_02_diff_1k.jpg` is an unmodified CC0 diffuse
photograph by Rob Tuytel / Poly Haven, retrieved 2026-10-07:
https://polyhaven.com/a/metal_plate_02 . It is used for subtle weathering,
recolored by the RIFT skin palette, without PBR/normal/roughness maps.
SHA-256: `6e80877d0e9d5973d96298c6091df7ace906b0a6760afc4f3592e4855f3f1d4c`.
