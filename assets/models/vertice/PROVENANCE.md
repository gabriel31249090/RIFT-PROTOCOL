# VERTICE visual pilot

These three Wavefront OBJ files are original RIFT Protocol meshes, authored in
this workspace on 2026-10-07. No Counter-Strike, MakeHuman or MPFB geometry,
textures, clothing, faces or animation data was copied into these files.

- `helmet.obj`: rigid open-face ceramic helmet, visor and earpieces; 30 triangles.
- `chest.obj`: bevelled front plate; 22 triangles.
- `impulse-pack.obj`: twin compact back modules; 36 triangles.

Units are metres, +Y is up and +Z is forward. OBJ materials are symbolic labels
resolved to the existing original RIFT surface atlas at runtime; no MTL or
third-party bitmap is needed. Standard OBJ winding is converted by MeshAssets.

The existing articulated Java rig still supplies the body, limbs, facial
geometry and all animation. The imported rigid pieces follow those existing
joints. This is not full skeletal skinning, a MakeHuman export, or a PBR model.
Detailed geometry is only used for the first VERTICE pilot; distant characters
keep the existing low-detail silhouette.

Files are distributed under the repository's MIT license (`LICENSE.txt`).
