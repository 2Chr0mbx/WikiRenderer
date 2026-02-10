package com.pigicial.wikirenderer.render.area.bounds.chunk;

import java.util.Set;

public record ChunkScanResult(Set<HorizontalMiniChunk> chunks, int minY, int maxY) {

}
