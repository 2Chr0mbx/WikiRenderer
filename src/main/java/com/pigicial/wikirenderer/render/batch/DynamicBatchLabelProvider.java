package com.pigicial.wikirenderer.render.batch;

import java.util.Collection;

public interface DynamicBatchLabelProvider {

    String buildFileName(String preset);

    Collection<String> buildPresetExamples();
}
