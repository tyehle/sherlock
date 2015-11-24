package cs.utah.sherlock.demos;

import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.util.CoreMap;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class StanfordCoRef {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        // creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER, parsing, and coreference resolution
        Properties props = new Properties();
        // using ner "muc7" model
        props.put("ner.model", "edu/stanford/nlp/models/ner/english.muc.7class.distsim.crf.ser.gz");

        props.put("annotators", "tokenize, ssplit, ner, parse, dcoref");
        props.setProperty("ner.useSUTime", "false");
        props.setProperty("ner.applyNumericClassifiers", "false");

        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        // read some text in the text variable
        //String text = "The atom is a basic unit of matter, it consists of a dense central nucleus surrounded by a cloud of negatively charged electrons."; // Add your text here!
        String text = "A middle school in Liverpool, Nova Scotia is pumping up bodies as well\n" +
                "as minds.\n" +
                "\n" +
                "It's an example of a school teaming up with the community to raise\n" +
                "money. South Queens Junior High School is taking aim at the fitness\n" +
                "market.\n" +
                "\n" +
                "The school has turned its one-time metal shop - lost to budget cuts\n" +
                "almost two years ago - into a money-making professional fitness club.\n" +
                "The club will be open seven days a week.\n" +
                "\n" +
                "The club, operated by a non-profit society made up of school and\n" +
                "community volunteers, has sold more than 30 memberships and hired a\n" +
                "full-time co-ordinator.\n" +
                "\n" +
                "Principal Betty Jean Aucoin says the club is a first for a Nova Scotia\n" +
                "public school. She says the school took it on itself to provide a\n" +
                "service needed in Liverpool.\n" +
                "\n" +
                "\"We don't have any athletic facilities here on the South Shore of Nova\n" +
                "Scotia, so if we don't use our schools, communities such as Queens are\n" +
                "going to be struggling to get anything going,\" Aucoin said.\n" +
                "\n" +
                "More than a $100,000 was raised through fund-raising and donations from\n" +
                "government, Sport Nova Scotia, and two local companies.\n" +
                "\n" +
                "Some people are wondering if the ties between the businesses and the\n" +
                "school are too close. Schools are not set up to make profits or promote\n" +
                "businesses.  \n" +
                "\n" +
                "Southwest Regional School Board superintendent Ann Jones says there's no\n" +
                "fear the lines between education and business are blurring.\n" +
                "\n" +
                "\"First call on any school facility belongs to... the youngsters in the\n" +
                "school,\" says Ann Jones.\n" +
                "\n" +
                "The 12,000-square-foot club has seven aerobic machines, including\n" +
                "treadmills, steppers, and stationary bicycles, as well as weight\n" +
                "machines and freeweights.\n" +
                "\n" +
                "Memberships cost $180 a year for adults and $135 for students and\n" +
                "seniors.\n" +
                "\n" +
                "Proceeds pay the salary of the centre co-ordinator and upkeep of the\n" +
                "facility.\n";
        // create an empty Annotation just with the given text
        Annotation document = new Annotation(text);

        // run all Annotators on this text
        pipeline.annotate(document);

        // these are all the sentences in this document
        // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);

        for(CoreMap sentence: sentences) {
            System.out.println("----------");
            // traversing the words in the current sentence
            // a CoreLabel is a CoreMap with additional token-specific methods
            for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
                // this is the text of the token
                String word = token.get(TextAnnotation.class);
                // this is the NER label of the token
                String ne = token.get(NamedEntityTagAnnotation.class);

                System.out.println(word+": "+ne);
            }
        }

        // This is the coreference link graph
        // Each chain stores a set of mentions that link to each other,
        // along with a method for getting the most representative mention
        // Both sentence and token offsets start at 1!
        Map<Integer, CorefChain> graph =
                document.get(CorefChainAnnotation.class);
        System.out.println(graph);
    }
}

