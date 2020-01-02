package tel.schich.estimationpoker;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;

import tel.schich.estimationpoker.message.CreateGameMessage;
import tel.schich.estimationpoker.message.EstimationMessage;
import tel.schich.estimationpoker.message.EstimationSelectedMessage;
import tel.schich.estimationpoker.message.JoinGameMessage;

@JsonSubTypes({
    @JsonSubTypes.Type(CreateGameMessage.class),
    @JsonSubTypes.Type(EstimationSelectedMessage.class),
    @JsonSubTypes.Type(EstimationMessage.class),
    @JsonSubTypes.Type(JoinGameMessage.class)
})
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
public interface Message {

    Type getType();

    int getId();

    Integer getResponseForId();

    enum Type {
        ESTIMATION_SELECTED(1),
        ESTIMATION(2),
        RESET_SELECTION(3),
        CREATE_GAME(4),
        JOIN_GAME(5);

        private static final Map<Integer, Type> TYPE_MAP;

        private final int id;

        Type(int id) {
            this.id = id;
        }

        @JsonValue
        int getId() {
            return this.id;
        }

        @JsonCreator
        public static Type byId(int id) {
            return TYPE_MAP.get(id);
        }

        static {
            Type[] values = Type.values();
            TYPE_MAP = new HashMap<>(values.length);
            for (Type value : values) {
                TYPE_MAP.put(value.getId(), value);
            }
        }
    }
}
