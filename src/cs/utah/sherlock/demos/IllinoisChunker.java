package cs.utah.sherlock.demos;

import cs.utah.sherlock.Util;
import edu.illinois.cs.cogcomp.lbj.chunk.Chunker;
import edu.illinois.cs.cogcomp.lbjava.nlp.Word;
import edu.illinois.cs.cogcomp.lbjava.nlp.seg.Token;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Documentation at http://cogcomp.cs.illinois.edu/page/software_view/Chunker
 * @author Tobin Yehle
 */
public class IllinoisChunker {
    public static void main(String[] args) {
        Chunker chunker = new Chunker();

        List<String> words = Util.listOf("This", "is", "a", "test", "sentence", ".",
                "Here", "is", "another", "with", "a", "prepositional", "phrase");

        List<Token> sentence = buildTokenList(words);

        List<String> tags = tagTokens(sentence, chunker);

        System.out.println("Generated tags: " + tags);

        System.out.println("Tagged sentence: " + prettyPrint(words, tags));

        System.out.println("Done");
    }

    /**
     * @param words The words in the sentence
     * @param tags The tags of the words
     * @return A string representing the tagged sequence
     */
    public static String prettyPrint(List<String> words, List<String> tags) {
        StringBuilder builder = new StringBuilder();

        String lastTag = null;
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            String tag = tags.get(i);

            if(lastTag != null && !lastTag.equals("O") && (tag.matches("B-.*") || tag.equals("O")))
                builder.append("]");

            if(lastTag != null)
                builder.append(" ");

            if(tag.matches("B-.*"))
                builder.append("["+tag.substring(2)+" ");

            builder.append(word);

            lastTag = tag;
        }

        if(lastTag != null && lastTag.matches("I-.*"))
            builder.append("]");

        return builder.toString();
    }

    /**
     * Tag the given list of tokens using the given chunker.
     * @param tokens The list of tokens to tag
     * @param chunker The phrase chunker
     * @return The list of tags for the given words
     */
    public static List<String> tagTokens(List<Token> tokens, Chunker chunker) {
        return tokens.stream().map(chunker::discreteValue).collect(Collectors.toList());
    }

    /**
     * Builds a list of illinois tokens from a list of strings.
     * @param words the words to make the tokens out of.
     * @return The list of tokens
     */
    public static List<Token> buildTokenList(List<String> words) {
        List<Token> out = Util.listOf();

        Token previous = null;
        Token current;
        Word currentWord;

        for(String word : words) {
            currentWord = new Word(word, previous);
            current = new Token(currentWord, previous, "");

            out.add(current);

            if(previous != null)
                previous.next = current;
            previous = current;
        }

        return out;
    }
}
