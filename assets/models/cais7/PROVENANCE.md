# CAIS-7 Visual Pilot

Three unmodified 1K diffuse photographs from Poly Haven, retrieved 2026-10-07,
are used as surface color, not as a PBR shader. Runtime mipmaps are capped at
256x256 and the floor is baked with the existing objective markings and shadows.

| File | Source | SHA-256 |
| --- | --- | --- |
| concrete_floor_worn_001_diff_1k.jpg | https://polyhaven.com/a/concrete_floor_worn_001 | 6e40c0fc908f4d66431836f5abe203f3a4eb064e88824378a91a46a26e2f464c |
| concrete_wall_003_diff_1k.jpg | https://polyhaven.com/a/concrete_wall_003 | 7d1d4b9f5ed1aa3e6385ef3232e26e123a3dac5f364e44f9f34784e39bf8d6bb |
| blue_metal_plate_diff_1k.jpg | https://polyhaven.com/a/blue_metal_plate | a0162bffce47d4a35613a12af22571b28c18412dc5805cbb69eac343554ef750 |

License: CC0 1.0, https://polyhaven.com/license and
https://creativecommons.org/publicdomain/zero/1.0/ . A local copy is included in
`CC0-1.0.txt`. Attribution is retained voluntarily; no website previews, logos,
addon code, normal maps, HDRIs or roughness maps are redistributed.

The original RIFT `service-unit.obj` has 24 vertices and 32 triangles. It was
authored for this project, not obtained from Poly Haven or Counter-Strike.
It follows the repository's game-content license. Twelve service units above
the exterior dock wall and two dock cranes are outside the playable boundary.
They do not introduce new colliders,
navigation obstacles, cover materials or actor hitboxes. Surface sound and
ballistics material IDs remain the established gameplay values.
