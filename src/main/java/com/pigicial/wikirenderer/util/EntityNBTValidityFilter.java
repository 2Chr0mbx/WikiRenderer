package com.pigicial.wikirenderer.util;

import net.minecraft.nbt.CompoundTag;

import java.util.function.BiPredicate;

public enum EntityNBTValidityFilter {
    NO_FILTER((starting, from) -> true),
    REQUIRE_ALL_APPLIED_VALID((starting, from) -> starting.keySet().stream().allMatch(from::contains)),
    REQUIRE_ONE_APPLIED_VALID((starting, from) -> starting.keySet().stream().anyMatch(from::contains)),
    REQUIRE_ALL_APPLIED_INVALID((starting, from) -> starting.keySet().stream().noneMatch(from::contains));

    private final BiPredicate<CompoundTag, CompoundTag> validityChecker;

    EntityNBTValidityFilter(BiPredicate<CompoundTag, CompoundTag> validityChecker) {
        this.validityChecker = validityChecker;
    }

    public boolean passesFilter(CompoundTag starting, CompoundTag fromEntity) {
        return this.validityChecker.test(starting, fromEntity);
    }
}
