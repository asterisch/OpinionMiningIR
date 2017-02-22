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
    private static int examine=CreateInverted.examine;
    private static RecordManager recMngr;
    private static PrimaryTreeMap<String,int[]> NBscores;
    private static final String tscode="1_total_class_scores_1";
    private static final String tcode="1_total_1";
    public static void main(String[] args)
    {
        clean(); // clear old database files
        System.out.println("Training");
        calculate_NB_scores(); // train system via lucene reverse index

        String temp1[],doc_id;int score,count=0,correct=0;BufferedReader in;double precision=0;
        PorterAnalyzer analyzer  = new PorterAnalyzer(new EnglishAnalyzer());
        TokenStream ts;
        int []scores;
        try {
            recMngr=new BaseRecordManager(recName);
            NBscores = recMngr.treeMap(recName);
            int[] total_class_scores=NBscores.get(tscode);
            int[] temp = NBscores.get(tcode);
            int total=temp[0];
            String line;StringBuilder ss;
        File folder = new File(data_path);
        for (File directory : folder.listFiles()) {
            if (directory.isDirectory()) {
                String dir_name=directory.getName();
                System.out.println("Analyzing: " + directory.getName());
                count=0;
                for (File file : directory.listFiles()) {
                    if (file.isFile() && !file.isHidden() && count<examine) { // && count<examine
                        ss=new StringBuilder();
                        temp1 = file.getName().split("_");
                        doc_id = temp1[0];
                        temp1 = temp1[1].split(".txt");
                        score = Integer.parseInt(temp1[0]);
                        try {
                            in = new BufferedReader(new FileReader(data_path + "/" + directory.getName() + "/" + file.getName()));
                            while ((line=in.readLine())!=null)
                            {
                                ss.append(line);
                            }
                            ts = analyzer.tokenStream("fieldName", new StringReader(ss.toString()));
                            ts.reset();
                            double classProb[] = new double[10];
                            for(int i=0;i<10;i++) {
                                classProb[i]=(double)total_class_scores[i]/(double)total;
                            }
                            int toclass=0;double max=0;
                            while (ts.incrementToken()) {
                                CharTermAttribute ca = ts.getAttribute(CharTermAttribute.class);
                                scores = NBscores.get(ca.toString());
                                if(scores==null) continue;
                                for(int i=0;i<10;i++)
                                {
                                    if(scores[i]!=0)
                                    classProb[i]*=Math.log10((double)total_class_scores[i]/(double)scores[i]);
                                }
                            }
                            for (int i=0;i<10;i++){
                            if(classProb[i]>max) {
                                max = classProb[i];
                                toclass = i + 1;
                            }}
                            if(score>5)
                            {
                                if(toclass>5) correct++;
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
                        ss.delete(0,ss.length());
                        ss=null;
                        count++;
                    }
                }
            }
        }
        System.out.println(correct+" "+2*count);
        System.out.println("Precision: "+(double)correct/(double)(2*count)); // count is of every class and totally there are 2 classes pos/neg
        recMngr.close();
        //clean();

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
            // Inita JDBM
            recMngr = new BaseRecordManager(recName);
            NBscores = recMngr.treeMap(recName);
            // Get terms from lucene's Inverted Index
            Terms terms = MultiFields.getTerms(reader,"text");
            TermsEnum term_iter= terms.iterator();
            // some vars
            BytesRef term = term_iter.next();
            int nb_class,periodic_commmit=0;
            int [] term_freqs,total_class_scores=new int[10],total=new int[1];
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
                    total_class_scores[nb_class-1]++;
                    total[0]++;

                }
                NBscores.put(term.utf8ToString(),term_freqs); // store term and it's count in score scale
                if(periodic_commmit==500) {
                    recMngr.commit();
                    periodic_commmit = 0; // Perform periodic commits on the database
                }
                periodic_commmit++;
                term=term_iter.next(); // next term returned from lucene
            }
            NBscores.put(tscode,total_class_scores);
            NBscores.put(tcode,total);
            // Calculate NB prior probabilities
            recMngr.close();

        } catch (IOException e) {
            System.out.println("[-] Can't parse InvertedIndex for NaiveBayes scores");
            e.printStackTrace();
        }
    }

    private static void clean()
    {
        File toClear=new File("NaiveBayes/");
        for(File f:toClear.listFiles())
        {
            f.delete();
        }
    }
}