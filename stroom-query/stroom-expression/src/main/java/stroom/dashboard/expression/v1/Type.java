package stroom.dashboard.expression.v1;

public interface Type {
    boolean isValue();

    boolean isNumber();

    boolean isError();

    boolean isNull();
}
