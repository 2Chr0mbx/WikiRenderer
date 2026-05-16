package com.pigicial.wikirenderer.render.area.bounds;

import com.pigicial.wikirenderer.render.area.WorldBlockMesh;
import com.pigicial.wikirenderer.render.area.side_view.ExpansionSide;
import com.pigicial.wikirenderer.render.area.side_view.MeshSideRotation;

public interface ExpandableMeshBounds extends MeshBounds {

    void move(WorldBlockMesh mesh, ExpansionSide side, MeshSideRotation currentRotation, int multiplier);
}
