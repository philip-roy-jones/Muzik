package xyz.philipjones.muzik.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.bson.types.ObjectId;

import java.io.IOException;

public class ObjectIdSerializer extends StdSerializer<ObjectId> {
    public ObjectIdSerializer() {
        super(ObjectId.class);
    }

    @Override
    public void serialize(ObjectId objectId, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(objectId.toHexString());
    }
}

