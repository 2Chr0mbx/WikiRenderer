package com.pigicial.wikirenderer.render.area.chunk;

import java.util.Set;

public record ChunkScanResult(Set<MiniChunk> chunks, int minY, int maxY) {

}
