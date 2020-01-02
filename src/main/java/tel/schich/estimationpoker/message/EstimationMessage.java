package tel.schich.estimationpoker.message;

import com.fasterxml.jackson.annotation.JsonCreator;

public class EstimationMessage extends BasicMessage {
    @JsonCreator
    public EstimationMessage(int id, Integer responseForId, Type type) {
        super(id, responseForId, type);
    }
}
