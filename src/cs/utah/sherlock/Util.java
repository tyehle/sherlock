package cs.utah.sherlock;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * Contains some helper methods to make life in Java bearable.
 * @author Tobin Yehle
 */
public class Util {
    public static class Pair<A, B> {
        private A a;
        private B b;

        public Pair(A a, B b) {
            this.a = a;
            this.b = b;
        }

        public A first() {
            return a;
        }

        public B second() {
            return b;
        }

        @Override
        public String toString() {
            return "(" + a + ", " + b + ")";
        }
    }

    public static <A, B> Pair<A, B> pairOf(A a, B b) {
        return new Pair<>(a, b);
    }

    @SafeVarargs
    public static <T> Set<T> setOf(T... things) {
        return new HashSet<>(Arrays.asList(things));
    }

    @SafeVarargs
    public static <T> List<T> listOf(T... things) {
        return new ArrayList<>(Arrays.asList(things));
    }

    @SafeVarargs
    public static <K, V> Map<K, V> mapOF(Pair<K, V>... things) {
        Map<K, V> out = new HashMap<>(things.length);
        for(Pair<K, V> pair : things) {
            out.put(pair.first(), pair.second());
        }
        return out;
    }

    /**
     * Reads lines from a file
     * @param filename The name of the file to read
     * @return A list of all the lines in the file
     */
    public static List<String> readLines(String filename) {
        ArrayList<String> out = new ArrayList<>();
        try(Scanner in  = new Scanner(new File(filename))) {
            while(in.hasNextLine()) {
                out.add(in.nextLine());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return out;
    }
}
