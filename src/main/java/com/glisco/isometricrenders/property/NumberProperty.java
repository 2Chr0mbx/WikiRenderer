package com.glisco.isometricrenders.property;

public abstract class NumberProperty<T extends Number> extends Property<T> {

    protected final T max;
    protected final T min;
    protected final T span;
    protected boolean allowRollover = false;

    protected NumberProperty(T defaultValue, T min, T max) {
        super(defaultValue);
        this.min = min;
        this.max = max;
        this.span = getSpan(min, max);
    }

    protected abstract T getSpan(T min, T max);

    public abstract void modify(double by);

    public abstract double progress() ;

    public abstract void setFromProgress(double progress);

    public T max() {
        return max;
    }

    public T min() {
        return min;
    }
}
