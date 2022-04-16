package myfindaction.myfindaction.stuff;

import kotlin.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class ListUtils {
    public static <T, S> S foldLeft(List<T> list, S start, BiFunction<S, T, S> f) {
        for (T t : list)
            start = f.apply(start, t);

        return start;
    }

    public static <S> List<Pair<S, Integer>> zipWithIndex(List<S> list) {
        var result = new ArrayList<Pair<S, Integer>>();
        for (int i = 0; i < list.size(); i++) {
            result.add(new Pair<>(list.get(i), i));
        }
        return result;
    }

}
