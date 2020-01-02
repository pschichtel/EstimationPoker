package tel.schich.estimationpoker.message;

import tel.schich.estimationpoker.Message;

public class BasicMessage implements Message {

    private final int id;
    private final Integer responseForId;
    private final Type type;

    public BasicMessage(int id, Integer responseForId, Type type) {
        this.id = id;
        this.responseForId = responseForId;
        this.type = type;
    }

    @Override
    public final Type getType() {
        return this.type;
    }

    @Override
    public final int getId() {
        return this.id;
    }

    @Override
    public Integer getResponseForId() {
        return this.responseForId;
    }
}
