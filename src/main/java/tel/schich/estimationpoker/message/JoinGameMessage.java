package tel.schich.estimationpoker.message;

import com.fasterxml.jackson.annotation.JsonCreator;

public class JoinGameMessage extends BasicMessage {
    @JsonCreator
    public JoinGameMessage(int id, Integer responseForId, Type type) {
        super(id, responseForId, type);
    }
}
