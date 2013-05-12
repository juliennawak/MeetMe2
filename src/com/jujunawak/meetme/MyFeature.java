package com.jujunawak.meetme;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mapfish.geo.MfFeature;
import org.mapfish.geo.MfGeometry;


public class MyFeature extends MfFeature {
    private final String id;
    private final MfGeometry geometry;
    private final JSONObject properties;

    public MyFeature(String id, MfGeometry geometry, JSONObject properties) {
        this.id = id;
        this.geometry = geometry;
        this.properties = properties;
    }

    public String getFeatureId() {
        return id;
    }

    public MfGeometry getMfGeometry() {
        return geometry;
    }

    public void toJSON(JSONWriter builder) throws JSONException {
        throw new RuntimeException("Not implemented");
    }
}
