package DEV_WORKERx86.WORKx86;

public class RabbitMsg {
    long deliveryTag;
    byte[] msg;
    String routingKey;
    String contentType;

    public RabbitMsg(long deliveryTag, byte[] msg, String routingKey, String contentType) {
        this.deliveryTag = deliveryTag;
        this.msg = msg;
        this.routingKey = routingKey;
        this.contentType = contentType;
    }

    public long getDeliveryTag() {
        return deliveryTag;
    }

    public void setDeliveryTag(long deliveryTag) {
        this.deliveryTag = deliveryTag;
    }

    public byte[] getMsg() {
        return msg;
    }

    public void setMsg(byte[] msg) {
        this.msg = msg;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}

