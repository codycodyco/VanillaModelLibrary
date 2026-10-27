package dev.cody.vanillamobs.api;

/** A deterministic match: the record and the display values that go with it. */
public record ModelSelection(ModelRecord model) {

    public ModelDisplay display() {
        return model.display();
    }

    public String modelId() {
        return model.modelId();
    }
}
