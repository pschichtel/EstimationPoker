package tel.schich.estimationpoker.message;

import com.fasterxml.jackson.annotation.JsonCreator;

public class EstimationSelectedMessage extends BasicMessage {
    @JsonCreator
    public EstimationSelectedMessage(int id, Integer responseForId, Type type) {
        super(id, responseForId, type);
    }
}
