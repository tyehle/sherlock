package cs.utah.sherlock.demos;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;

import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Tobin Yehle
 */
public class StanfordTokenizer {

    public static List<CoreLabel> tokenizeString(String input) {
        PTBTokenizer<CoreLabel> tokenizer = new PTBTokenizer<>(new StringReader(input),
                new CoreLabelTokenFactory(), "invertible=true");
        List<CoreLabel> out = new ArrayList<>();
        while(tokenizer.hasNext()) {
            out.add(tokenizer.next());
        }
        return out;
    }

    public static void main(String[] args) throws FileNotFoundException {
        String document = "This is a random-ass test sentence that doesn't keep anything\tsacred ! NOTHING!";

        // option #1: By sentence.
        DocumentPreprocessor dp = new DocumentPreprocessor(new StringReader(document));
        for (List<HasWord> sentence : dp) {
            System.out.println(sentence);
        }

        // option #2: By token
        tokenizeString(document).stream().forEach(word -> System.out.print(word+"  "));
    }
}
