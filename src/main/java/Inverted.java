import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.tartarus.snowball.ext.PorterStemmer;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by steve on 16/2/2017.
 */
public class Inverted
{
    public static String index_path="/tmp/inverted_index";
    public static String data_path = "/home/steve/IdeaProjects/ProjectIR/data/train";
    private static List<String> stopwords = Arrays.asList("a", "able", "about",
            "across", "after", "all", "almost", "also", "am", "among", "an",
            "and", "any", "are", "as", "at", "be", "because", "been", "but",
            "by", "can", "cannot", "could", "dear", "did", "do", "does",
            "either", "else", "ever", "every", "for", "from", "get", "got",
            "had", "has", "have", "he", "her", "hers", "him", "his", "how",
            "however", "i", "if", "in", "into", "is", "it", "its", "just",
            "least", "let", "like", "likely", "may", "me", "might", "most",
            "must", "my", "neither", "no", "nor", "not", "of", "off", "often",
            "on", "only", "or", "other", "our", "own", "rather", "said", "say",
            "says", "she", "should", "since", "so", "some", "than", "that",
            "the", "their", "them", "then", "there", "these", "they", "this",
            "tis", "to", "too", "twas", "us", "wants", "was", "we", "were",
            "what", "when", "where", "which", "while", "who", "whom", "why",
            "will", "with", "would", "yet", "you", "your");
    public static void main(String[] args)
    {
        if (args.length > 1 && args.length < 3)
        {
            index_path=args[1];
            data_path=args[2];
        }
        System.err.println(index_path+" "+data_path);
        String temp1[];
        String doc_id;
        int score,count=0;
        Directory dir = null;
        HashMap<String,List<Integer>> inverted = new HashMap<String, List<Integer>>();
            BufferedReader in;
            File folder = new File(data_path);
            for (File directory : folder.listFiles()) {
                if (directory.isDirectory()) {
                    System.out.println("Analyzing: " + directory.getName());
                    for (File file : directory.listFiles()) {
                        if (file.isFile() && !file.isHidden() && count < 1000) {
                            temp1 = file.getName().split("_");
                            doc_id = temp1[0];
                            temp1 = temp1[1].split(".txt");
                            score = Integer.parseInt(temp1[0]);
                            try {
                                in = new BufferedReader(new FileReader(data_path + "/" + directory.getName() + "/" + file.getName()));
                                StandardTokenizer tokenizer = new StandardTokenizer();
                                tokenizer.setReader(new StringReader(in.readLine().toString()));
                                tokenizer.reset();
                                CharTermAttribute attr = tokenizer.addAttribute(CharTermAttribute.class);
                                while(tokenizer.incrementToken()) {
                                    // Grab the term
                                    String term = attr.toString();
                                    if(!stopwords.contains(term))
                                    {
                                        PorterStemmer p = new PorterStemmer();
                                        List<Integer> scrs = inverted.get(term);
                                        if(scrs == null)
                                        {
                                            scrs= new ArrayList<Integer>();
                                        }
                                        scrs.add(score);
                                    }
                                    // Do something crazy...
                                }
                            } catch (FileNotFoundException e) {
                                System.err.println("[-] File not found "+file.getName());
                                e.printStackTrace();
                            } catch (IOException e) {
                                System.err.println("[-] Error reading file "+file.getName());
                                e.printStackTrace();
                            }

                            count++;
                        }


                    }
                }
            }


    }
}
