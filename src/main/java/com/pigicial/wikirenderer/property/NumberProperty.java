package com.pigicial.wikirenderer.property;

public abstract class NumberProperty<T extends Number> extends Property<T> {

    protected T max;
    protected final T min;
    protected T span;
    protected boolean allowRollover = false;

    protected NumberProperty(T defaultValue, T min, T max) {
        super(defaultValue);
        this.min = min;
        this.max = max;
        this.span = getSpan(min, max);
    }

    public boolean hasRollover() {
        return allowRollover;
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
