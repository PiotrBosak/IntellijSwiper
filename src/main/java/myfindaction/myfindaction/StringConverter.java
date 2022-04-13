package myfindaction.myfindaction;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

//fix it
public class StringConverter {

    List<String> list = new ArrayList<String>();
    private static final List<String> specials = List.of("\\", "^", "$", ".", "|", "?", "*", "+", "(", ")", "[", "]", "{", "}");

    public String convert(String s) {
        s = this.replaceSpecialCharacters(s);
         return s.replaceAll("\\s+", ".*");
    }
    public String convertPermutations(String s){
        s = this.replaceSpecialCharacters(s);
        String[] words = s.split("\\s+");
        this.printAllRecursive(words.length, words, ".*");
        return makeString(this.list, "|");
    }

    private String replaceSpecialCharacters(String s) {
        var separate = new ArrayList<String>();
        for(int i = 0; i < s.length(); i++){
            separate.add(s.substring(i,i+1));
        }
        return separate.stream()
                .map(str -> {
                    for (int j = 0; j < specials.size(); j++) {
                        str = str.equals(specials.get(j)) ? "\\" + str : str;
                    }
                    return str;
                })
                .collect(Collectors.joining());

    }



    private void printAllRecursive(int n, String[] elements, String delimiter) {
        if (n == 1) {
            this.list.add(makeString(Arrays.stream(elements).collect(Collectors.toList()), delimiter));
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
        for (T t : list)
            start = f.apply(start, t);

        return start;
    }
    public static <S> List<S> tail(List<S> list) {
        return list.subList(1, list.size());
    }

}