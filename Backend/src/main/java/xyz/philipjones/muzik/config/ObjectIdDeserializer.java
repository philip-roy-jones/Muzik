package xyz.philipjones.muzik.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.bson.types.ObjectId;

import java.io.IOException;

public class ObjectIdDeserializer extends StdDeserializer<ObjectId> {

    public ObjectIdDeserializer() {
        super(ObjectId.class);
    }

    @Override
    public ObjectId deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return new ObjectId(p.getText());
    }
}
