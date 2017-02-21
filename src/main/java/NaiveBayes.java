import jdbm.PrimaryTreeMap;
import jdbm.RecordManager;
import jdbm.recman.BaseRecordManager;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.*;
import java.nio.file.Paths;
import java.util.HashSet;

/**
 * Created by steve on 18/2/2017.
 */
public class NaiveBayes
{
    public static String data_path = "/home/steve/IdeaProjects/ProjectIR/data/train";
    private static String recName="NaiveBayes/NB_scores";
    private static int examine=10000;
    private static RecordManager recMngr;
    private static PrimaryTreeMap<String,int[]> NBscores;
    public static void main(String[] args)
    {
        calculate_NB_scores();

        String temp1[],doc_id;int score,count=0,total=0,correct=0;BufferedReader in;double precision=0;
        PorterAnalyzer analyzer  = new PorterAnalyzer(new EnglishAnalyzer());
        TokenStream ts;
        int[] total_class_scores=new int[10],scores;
        HashSet<Integer>[] dist = new HashSet [10];
        try {
            recMngr=new BaseRecordManager(recName);
            NBscores = recMngr.treeMap(recName);
        File folder = new File(data_path);
        for (File directory : folder.listFiles()) {
            if (directory.isDirectory()) {
                String dir_name=directory.getName();
                System.out.println("Analyzing: " + directory.getName());
                for (File file : directory.listFiles()) {
                    if (file.isFile() && !file.isHidden() && count < examine) {
                        for(int i=0;i<10;i++) dist[i]=new HashSet<Integer>();
                        temp1 = file.getName().split("_");
                        doc_id = temp1[0];
                        temp1 = temp1[1].split(".txt");
                        score = Integer.parseInt(temp1[0]);
                        try {
                            in = new BufferedReader(new FileReader(data_path + "/" + directory.getName() + "/" + file.getName()));
                            ts = analyzer.tokenStream("fieldName", new StringReader(in.readLine()));
                            ts.reset();
                            while (ts.incrementToken()) {
                                CharTermAttribute ca = ts.getAttribute(CharTermAttribute.class);
                                scores = NBscores.get(ca.toString());
                                for(int i=0;i<10;i++)
                                {
                                    if(scores[i]!=0) dist[i].add(scores[i]);
                                    total_class_scores[i]+=scores[i];
                                    total+=scores[i];
                                }
                            }
                            int toclass=0;double max=0,prob=1;
                            for(int i=0;i<10;i++) {
                                prob=1;
                                double classProp=(double) total_class_scores[i]/(double)total;
                                for (Integer num : dist[i])
                                {
                                    prob*=Math.log10((double)num/(double)total_class_scores[i]);
                                }
                                prob*=classProp;
                                if(prob>max)
                                {
                                    max=prob;
                                    toclass=i+1;
                                }
                            }
                            //System.out.println(doc_id+" "+score+" "+toclass);
                            if(dir_name.equals("pos"))
                            {
                                if(toclass>=5) correct++;
                            }
                            else
                            {
                                if(toclass<5) correct++;
                            }

                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        count++;
                    }
                }
            }
        }
            System.out.println("Precision: "+(double)correct/(double)examine);
        recMngr.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

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
            //RecordManagerFactory.createRecordManager(recName);
            //RecordManager recMngr = new CacheRecordManager(new BaseRecordManager(recName),1000,true);
            recMngr = new BaseRecordManager(recName);
            NBscores = recMngr.treeMap(recName);
            // Get terms from lucene's Inverted Index
            Terms terms = MultiFields.getTerms(reader,"text");
            TermsEnum term_iter= terms.iterator();
            // some vars
            BytesRef term = term_iter.next();
            int nb_class,periodic_commmit=0;
            int [] term_freqs;
            // Calculate Naive Bayes score of each term
            while (term!=null)
            {
                q = new TermQuery(new Term("text", term.utf8ToString()));
                th = new TotalHitCountCollector();
                searcher.search(q,th);
                docs = searcher.search(q,Math.max(1,th.getTotalHits()));
                term_freqs=new int[10];
                for(int i=0;i<docs.scoreDocs.length;i++)
                {
                    doc = searcher.doc(docs.scoreDocs[i].doc);
                    nb_class=Integer.valueOf(doc.get("score"));
                    term_freqs[nb_class-1]++; // count term's frequency in every class
                }
                NBscores.put(term.utf8ToString(),term_freqs); // store term and it's count in score scale
                if(periodic_commmit==300) recMngr.commit();periodic_commmit=0; // Perform periodic commits on the database
                periodic_commmit++;
                term=term_iter.next(); // next term returned from lucene
            }
            // Calculate NB prior probabilities
            recMngr.close();

        } catch (IOException e) {
            System.out.println("[-] Can't parse InvertedIndex for NaiveBayes scores");
            e.printStackTrace();
        }
    }

}