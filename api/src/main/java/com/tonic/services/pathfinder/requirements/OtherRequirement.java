package com.tonic.services.pathfinder.requirements;

import java.util.function.Supplier;

public class OtherRequirement implements Requirement {
    private final Supplier<Boolean> func;
    public OtherRequirement(Supplier<Boolean> func) {
        this.func = func;
    }
    @Override
    public Boolean get() {
        return func.get();
    }
}
