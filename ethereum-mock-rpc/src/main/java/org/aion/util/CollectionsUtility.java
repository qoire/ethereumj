package org.aion.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.BiFunction;

public class CollectionsUtility {

    @Data
    @AllArgsConstructor
    public static class Intersect<X, Y> {
        @NonNull
        private final X x;
        @NonNull
        private final Y y;
    }

    public static <X, Y>
    Collection<Intersect<X,Y>> intersection(
            @Nonnull final Collection<X> x,
            @Nonnull final Collection<Y> y,
            @Nonnull final BiFunction<X, Y, Boolean> func) {
        // TODO: there should be a better way than O(n^2)
        // see: https://stackoverflow.com/questions/14337067/faster-methods-for-set-intersection
        var inter = new ArrayList<Intersect<X, Y>>();
        for (X _x : x) {
            for (Y _y : y) {
                if (func.apply(_x, _y))
                    inter.add(new Intersect<>(_x, _y));
            }
        }
        return inter;
    }
}
