package org.example.model;

public class Message {
    private final long id;
    private final String field1;
    private final String field2;
    private final String field3;
    private final String field4;
    private final String field5;
    private final String field6;
    private final String field7;
    private final String field8;
    private final String field9;
    private final String field10;
    private final String field11;
    private final String field12;
    private final ObjectForMessage field13;

    private Message(Builder builder) {
        this.id = builder.id;
        this.field1 = builder.field1;
        this.field2 = builder.field2;
        this.field3 = builder.field3;
        this.field4 = builder.field4;
        this.field5 = builder.field5;
        this.field6 = builder.field6;
        this.field7 = builder.field7;
        this.field8 = builder.field8;
        this.field9 = builder.field9;
        this.field10 = builder.field10;
        this.field11 = builder.field11;
        this.field12 = builder.field12;
        this.field13 = builder.field13;
    }

    public long getId() {
        return id;
    }

    public String getField1() {
        return field1;
    }

    public String getField2() {
        return field2;
    }

    public String getField3() {
        return field3;
    }

    public String getField4() {
        return field4;
    }

    public String getField5() {
        return field5;
    }

    public String getField6() {
        return field6;
    }

    public String getField7() {
        return field7;
    }

    public String getField8() {
        return field8;
    }

    public String getField9() {
        return field9;
    }

    public String getField10() {
        return field10;
    }

    public String getField11() {
        return field11;
    }

    public String getField12() {
        return field12;
    }

    public ObjectForMessage getField13() {
        return field13;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message message = (Message) o;

        return id == message.id;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    @Override
    public String toString() {
        return "Message{" + "id="
                + id + ", field1='"
                + field1 + '\'' + ", field2='"
                + field2 + '\'' + ", field3='"
                + field3 + '\'' + ", field4='"
                + field4 + '\'' + ", field5='"
                + field5 + '\'' + ", field6='"
                + field6 + '\'' + ", field7='"
                + field7 + '\'' + ", field8='"
                + field8 + '\'' + ", field9='"
                + field9 + '\'' + ", field10='"
                + field10 + '\'' + ", field11='"
                + field11 + '\'' + ", field12='"
                + field12 + '\'' + ", field13="
                + field13 + '}';
    }

    public static Builder builder(long id) {
        return new Builder(id);
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static class Builder {
        private final long id;
        private String field1;
        private String field2;
        private String field3;
        private String field4;
        private String field5;
        private String field6;
        private String field7;
        private String field8;
        private String field9;
        private String field10;
        private String field11;
        private String field12;
        private ObjectForMessage field13;

        public Builder(long id) {
            this.id = id;
        }

        public Builder(Message message) {
            this.id = message.id;
            this.field1 = message.field1;
            this.field2 = message.field2;
            this.field3 = message.field3;
            this.field4 = message.field4;
            this.field5 = message.field5;
            this.field6 = message.field6;
            this.field7 = message.field7;
            this.field8 = message.field8;
            this.field9 = message.field9;
            this.field10 = message.field10;
            this.field11 = message.field11;
            this.field12 = message.field12;
            this.field13 = message.field13;
        }

        public Builder field1(String field1) {
            this.field1 = field1;
            return this;
        }

        public Builder field2(String field2) {
            this.field2 = field2;
            return this;
        }

        public Builder field3(String field3) {
            this.field3 = field3;
            return this;
        }

        public Builder field4(String field4) {
            this.field4 = field4;
            return this;
        }

        public Builder field5(String field5) {
            this.field5 = field5;
            return this;
        }

        public Builder field6(String field6) {
            this.field6 = field6;
            return this;
        }

        public Builder field7(String field7) {
            this.field7 = field7;
            return this;
        }

        public Builder field8(String field8) {
            this.field8 = field8;
            return this;
        }

        public Builder field9(String field9) {
            this.field9 = field9;
            return this;
        }

        public Builder field10(String field10) {
            this.field10 = field10;
            return this;
        }

        public Builder field11(String field11) {
            this.field11 = field11;
            return this;
        }

        public Builder field12(String field12) {
            this.field12 = field12;
            return this;
        }

        public Builder field13(ObjectForMessage field13) {
            this.field13 = field13;
            return this;
        }

        public Message build() {
            return new Message(this);
        }
    }
}
