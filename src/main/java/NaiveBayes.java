import jdbm.PrimaryTreeMap;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.recman.BaseRecordManager;
import jdbm.recman.CacheRecordManager;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefHash;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by steve on 18/2/2017.
 */
public class NaiveBayes
{
    public static String data_path = "/home/steve/IdeaProjects/ProjectIR/data/train";
    private static String score_path="NaiveBayes/NB_scores";
    public static int examine=10000;
    public static void main(String[] args)
    {
        calculate_NB_scores();


    }
    private static void calculate_NB_scores()
    {
        Directory dir=null;
        try {
            dir = FSDirectory.open(Paths.get(CreateInverted.index_path));
        }catch (IOException e)
        {
            System.err.println("[+] Error writing CreateInverted Index to "+CreateInverted.index_path);
            System.exit(-1);
        }
        DirectoryReader reader=null;
        try {
            reader = DirectoryReader.open(dir);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        IndexSearcher searcher = new IndexSearcher(reader);
        //BytesRefHash h = new BytesRefHash();

        TermQuery q;TotalHitCountCollector th;TopDocs docs;Document doc;
        try {
            // Init JDBM PrimaryTree and RecordManager
            String recName=score_path;
            //RecordManagerFactory.createRecordManager(recName);
            //RecordManager recMngr = new CacheRecordManager(new BaseRecordManager(recName),1000,true);
            RecordManager recMngr = new BaseRecordManager(recName);
            PrimaryTreeMap<String,float[]> NBscores = recMngr.treeMap(recName);
            // Get terms from lucene's Inverted Index
            Terms terms = MultiFields.getTerms(reader,"text");
            TermsEnum term_iter= terms.iterator();
            // some vars
            BytesRef term = term_iter.next();
            int nb_class,periodic_commmit=0;
            int[] classCount=new int[10];
            float[] term_freqs;
            // Calculate Naive Bayes score of each term
            while (term!=null)
            {
                q = new TermQuery(new Term("text", term.utf8ToString()));
                th = new TotalHitCountCollector();
                searcher.search(q,th);
                docs = searcher.search(q,Math.max(1,th.getTotalHits()));
                term_freqs=new float[10];
                for(int i=0;i<docs.scoreDocs.length;i++)
                {
                    doc = searcher.doc(docs.scoreDocs[i].doc);
                    nb_class=Integer.valueOf(doc.get("score"));
                    classCount[nb_class-1]++; //count total words in class
                    term_freqs[nb_class-1]++; // count term's frequency in every class
                }
                NBscores.put(term.utf8ToString(),term_freqs); // store term and it's count in score scale
                if(periodic_commmit==300) recMngr.commit();periodic_commmit=0; // Perform periodic commits on the database
                periodic_commmit++;
                term=term_iter.next(); // next term returned from lucene
            }
            // Calculate NB prior probabilities
            float[] scores;
            for(String key : NBscores.keySet())
            {
                scores = NBscores.get(key);
                System.out.printf(key+" ");
                for(int i=0;i<10;i++)
                {
                    if(classCount[i]!=0)
                    scores[i]/=classCount[i];
                    System.out.printf(scores[i]+" ");
                }
                System.out.printf("\n");
            }
            recMngr.close();

        } catch (IOException e) {
            System.out.println("[-] Can't parse InvertedIndex for NaiveBayes scores");
            e.printStackTrace();
        }
    }

}