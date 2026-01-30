package com.pigicial.wikirenderer.property;

import net.minecraft.util.Mth;

public class IntProperty extends NumberProperty<Integer> {

    private IntProperty(int defaultValue, int min, int max) {
        super(defaultValue, min, max);
    }

    @Override
    protected Integer getSpan(Integer min, Integer max) {
        return max - min;
    }

    public static IntProperty of(int defaultValue, int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("'min' must be less than 'max'");
        }

        return new IntProperty(defaultValue, min, max);
    }

    public IntProperty withRollover() {
        this.allowRollover = true;
        return this;
    }

    @Override
    public void set(Integer value) {
        if (allowRollover && (value > this.max || value < this.min)) {
            if (value > this.max) {
                value = (value - this.max) % this.max;
            } else {
                int range = this.max - this.min;
                value = this.max - ((this.min - value) % range);
            }
        }

        super.set(value);
    }

    @Override
    public void modify(double byDouble) {
        int by = (int) Math.round(byDouble);
        if (this.allowRollover) {
            this.value += by;
            if (this.value > this.max) this.value -= this.span;
            if (this.value < this.min) this.value += this.span;
        } else {
            this.value = Mth.clamp(this.value + by, this.min, this.max);
        }

        this.invokeListeners();
    }

    @Override
    public double progress() {
        return (this.value - this.min) / (double) this.span;
    }

    @Override
    public void setFromProgress(double progress) {
        this.value = (int) Math.round(this.min + progress * this.span);
        this.invokeListeners();
    }
}
