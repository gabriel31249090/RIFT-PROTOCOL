# VERTICE visual pilot

These three Wavefront OBJ files are original RIFT Protocol meshes, authored in
this workspace on 2026-10-07. No Counter-Strike, MakeHuman or MPFB geometry,
clothing, faces or animation data was copied into these files.

- `helmet.obj`: rigid open-face ceramic helmet, visor and earpieces; 30 triangles.
- `chest.obj`: bevelled front plate; 22 triangles.
- `impulse-pack.obj`: twin compact back modules; 36 triangles.

Units are metres, +Y is up and +Z is forward. OBJ materials are symbolic labels
resolved to the original RIFT atlas and the licensed photographic pilot
materials at runtime; no MTL file is needed. Standard OBJ winding is converted by MeshAssets.

The existing articulated Java rig still supplies the body, limbs, facial
geometry and all animation. The imported rigid pieces follow those existing
joints. This is not full skeletal skinning, a MakeHuman export, or a PBR model.
Detailed geometry is only used for the first VERTICE pilot; distant characters
keep the existing low-detail silhouette.

Files are distributed under the repository's MIT license (`LICENSE.txt`).

The fabric bitmap `fabric_leather_02_diff_1k.jpg` is an unmodified CC0 diffuse
photograph by Rob Tuytel / Poly Haven, retrieved 2026-10-07:
https://polyhaven.com/a/fabric_leather_02 . RIFT recolors its grain for dark
tactical fabric/soft equipment; it is not a PBR material or a clothing mesh.
SHA-256: `97ba9480c7619efc9b8c08202c3e3773d1dd532dacce4dfe97f86e95339cf132`.
Armor uses the shared metal bitmap documented in `../echo/README.md`.
The shared CC0 license copy is in `../echo/CC0-1.0.txt`; all pilot mesh/bitmap
hashes are in `SHA256SUMS.txt`.
