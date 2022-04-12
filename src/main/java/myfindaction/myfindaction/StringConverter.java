package myfindaction.myfindaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

//fix it
public class StringConverter {
    List<String> list = new ArrayList();
    private static final List<String> specials = List.of(new String[]{"\\", "^", "$", ".", "|", "?", "*", "+", "(", ")", "[", "]", "{", "}"});

    public String convert(String s) {
        s = this.replaceSpecialCharacters(s);
        String[] words = s.split("\\s+");
        this.printAllRecursive(words.length, words, ".*");
        return makeString(this.list, "|");
    }

    private void printAllRecursive(int n, String[] elements, String delimiter) {
        if (n == 1) {
            this.list.add(makeString((List) Arrays.stream(elements).collect(Collectors.toList()), delimiter));
        } else {
            for (int i = 0; i < n - 1; ++i) {
                this.printAllRecursive(n - 1, elements, delimiter);
                if (n % 2 == 0) {
                    this.swap(elements, i, n - 1);
                } else {
                    this.swap(elements, 0, n - 1);
                }
            }

            this.printAllRecursive(n - 1, elements, delimiter);
        }

    }

    private String replaceSpecialCharacters(String s) {
        String result = s;
        String sp;
        List<String> parts;


        return result;
    }

    private void swap(String[] input, int a, int b) {
        String tmp = input[a];
        input[a] = input[b];
        input[b] = tmp;
    }

    public static String makeString(List<String> list, String separator) {
        if (list.isEmpty()) {
            return "";
        } else {
            StringBuilder builder = new StringBuilder((String) list.get(0));
            return ((StringBuilder) foldLeft(tail(list), builder, (z, elem) -> {
                z.append(separator);
                z.append(elem);
                return z;
            })).toString();
        }
    }

    public static <T, S> S foldLeft(List<T> list, S start, BiFunction<S, T, S> f) {
        for(int i = 0; i < list.size(); i++) {
            start = f.apply(start,list.get(i));
        }
        return start;
    }

    public static <S> List<S> tail(List<S> list) {
        return list.subList(1, list.size());
    }
}