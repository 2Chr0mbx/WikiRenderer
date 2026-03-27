package com.pigicial.wikirenderer.util;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.mojang.util.UUIDTypeAdapter;

import java.io.IOException;
import java.util.UUID;

public class NullSafeUUIDTypeAdapter extends UUIDTypeAdapter {

    @Override
    public void write(JsonWriter out, UUID value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            super.write(out, value);
        }
    }

    @Override
    public UUID read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        return super.read(in);
    }
}
