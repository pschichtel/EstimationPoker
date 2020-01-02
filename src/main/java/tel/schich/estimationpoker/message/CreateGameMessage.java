package tel.schich.estimationpoker.message;

import com.fasterxml.jackson.annotation.JsonCreator;

public class CreateGameMessage extends BasicMessage {
    @JsonCreator
    public CreateGameMessage(int id, Integer responseForId, Type type) {
        super(id, responseForId, type);
    }
}
