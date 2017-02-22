import jdbm.PrimaryTreeMap;
import jdbm.RecordManager;
import jdbm.recman.BaseRecordManager;
import jdbm.recman.CacheRecordManager;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by steve on 21/2/2017.
 */
public class KNN
{

    public static String recNametfidf ="Knn/tfidf_vectors";
    private static String recNameidf="Knn/terms_idf";
    private static String recNameSim="Knn/similarities";
    private static int K=5;
    private static int examine=CreateInverted.examine;
    private static RecordManager recMngridf;
    private static RecordManager recMngrtfidf;
    private static RecordManager recMngrSim;
    private static PrimaryTreeMap<Integer,HashMap<String,Double>> tfidf_vectors;
    private static PrimaryTreeMap<String,Double> idf_vectors;
    private static PrimaryTreeMap<Integer,Double> similarites;

    public static void main(String[] args)
    {
        clean();
        System.out.println("Training");
        train();
        int count=0,correct=0;BufferedReader in;
        PorterAnalyzer analyzer  = new PorterAnalyzer(new EnglishAnalyzer());
        TokenStream ts;
        HashMap<String,Double> docQuery=null;
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

        try {
            recMngrtfidf=new BaseRecordManager(recNametfidf);
            recMngridf= new BaseRecordManager(recNameidf);
           // recMngrSim = new BaseRecordManager(recNameSim);
            tfidf_vectors = recMngrtfidf.treeMap(recNametfidf);
            idf_vectors=recMngridf.treeMap(recNameidf);
           // similarites=recMngrSim.treeMap(recNameSim);
            HashSet<Integer> docs_to_examine;
            HashMap<String,Double> index_doc;
            String line;StringBuilder ss;
            double normQ,normD,dot,sim,tf,index_idf,qIdf,maxtf;
            String term;
            Document doc;int pos,neg;
            TermQuery q;TotalHitCountCollector th;TopDocs docs;

            File folder = new File(CreateInverted.data_path);
            for (File directory : folder.listFiles()) {
                if (directory.isDirectory()) {
                    String dir_name=directory.getName();
                    System.out.println("Analyzing: " + directory.getName());
                    count=0;
                    for (File file : directory.listFiles()) {
                        if (file.isFile() && !file.isHidden() && count<examine) {
                            ss=new StringBuilder();
                            docQuery=new HashMap<String, Double>();
                            docs_to_examine=new HashSet<Integer>();

                            try {
                                in = new BufferedReader(new FileReader(CreateInverted.data_path + "/" + directory.getName() + "/" + file.getName()));
                                while ((line=in.readLine())!=null)
                                {
                                    ss.append(line);
                                }
                                ts = analyzer.tokenStream("fieldName", new StringReader(ss.toString()));
                                ts.reset();
                                while (ts.incrementToken()) {
                                    CharTermAttribute ca = ts.getAttribute(CharTermAttribute.class);
                                    term = ca.toString();
                                    if(docQuery.containsKey(term))
                                    {
                                        tf = docQuery.get(term).doubleValue();
                                        docQuery.put(term,++tf);
                                    }
                                    else
                                    {
                                        // Examine for tok K similar docs only the docs that contain at least one term of the queryDoc
                                        docQuery.put(term,1.0);
                                        q = new TermQuery(new Term("text", term));
                                        th = new TotalHitCountCollector();
                                        searcher.search(q,th);
                                        docs = searcher.search(q,Math.max(1,th.getTotalHits()));
                                        for(int i=0;i<docs.scoreDocs.length;i++) {
                                            docs_to_examine.add(docs.scoreDocs[i].doc);
                                        }
                                    }
                                }
                                maxtf=Collections.max(docQuery.values());
                                // Calculate query tf idf scores (Salton & Buckley method)
                                for(String trm: docQuery.keySet())
                                {
                                    index_idf = idf_vectors.get(trm).doubleValue();
                                    qIdf= ( 0.5 * (docQuery.get(trm) / maxtf) + 0.5 )*index_idf;
                                    docQuery.put(trm,qIdf);
                                }
                                // find top k with cosine similarity


                                int[] topKID = new int[K];
                                double[] topKsims= new double[K];
                                for(Integer i : docs_to_examine) // examine only possible relevant docs
                                {
                                    index_doc=tfidf_vectors.get(i);
                                    dot=0;normD=0;normQ=0;
                                    for(String t:docQuery.keySet())
                                    {
                                        if(index_doc.containsKey(t))
                                        {
                                            dot+=docQuery.get(t).doubleValue()*index_doc.get(t).doubleValue();
                                        }
                                        normQ+=Math.pow(docQuery.get(t).doubleValue(),2);
                                    }
                                    for(String t:index_doc.keySet())
                                    {
                                        normD+=Math.pow(index_doc.get(t).doubleValue(),2);
                                    }
                                    sim=dot/ ( Math.sqrt(normD)*Math.sqrt(normQ) );
                                    for(int j=0;j<K;j++)
                                    {
                                        if(sim>topKsims[j])
                                        {
                                            topKID[j]=i;
                                            topKsims[j]=sim;
                                            break;
                                        }
                                    }
                                }
                                pos=0;neg=0;
                                for(int i=0;i<K;i++)
                                {
                                    int id=topKID[i];
                                    doc=reader.document(id);
                                    if(Integer.valueOf(doc.get("score"))>=7) pos++;
                                    else neg++;
                                }
                                if(dir_name.equals("pos"))
                                {
                                    if(pos>=neg) correct++;
                                }
                                else
                                {
                                    if(pos<neg) correct++;
                                }

                                //recMngrSim.commit();


                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            ss.delete(0,ss.length());
                            docQuery=null;
                            ss=null;
                            count++;
                        }
                    }
                }
            }
            System.out.println(correct+" "+(2*count));
            System.out.println("Precision: "+(double)correct/(double)(2*count));
            recMngridf.close();
            recMngrtfidf.close();
            reader.close();
            dir.close();
            //clean();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void train()
    {
        // Open Lucene
        Directory dir=null;
        DirectoryReader reader=null;
        try {
            dir = FSDirectory.open(Paths.get(CreateInverted.index_path));
        }catch (IOException e)
        {
            System.err.println("[+] Error writing CreateInverted Index to "+CreateInverted.index_path);
            System.exit(-1);
        }
        try {
            reader = DirectoryReader.open(dir);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        IndexSearcher searcher = new IndexSearcher(reader);
        Query q;TotalHitCountCollector th;TopDocs docs;Document doc;
        // Open term_idf
        try {
            recMngridf = new BaseRecordManager(recNameidf);
            recMngrtfidf = new BaseRecordManager(recNametfidf);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        idf_vectors = recMngridf.treeMap(recNameidf);
        tfidf_vectors = recMngrtfidf.treeMap(recNametfidf);

        try {
            double nidf,tf,tf_idf,maxtf;
            long freq;
            int doc_id,periodic_commmit=0;
            HashMap<String,Double> tf_map;
            Terms termsVector;
            TermsEnum itr;
            BytesRef vterm;
            // query all docs
            q = new MatchAllDocsQuery();
            docs = searcher.search(q,reader.numDocs());
            // For each document
            for(int i=0;i<docs.scoreDocs.length;i++)
            {
                doc_id = docs.scoreDocs[i].doc;
                // get term vectors
                termsVector = reader.getTermVector(doc_id,"text");
                if(termsVector==null) continue;
                itr=termsVector.iterator();
                vterm=itr.next();
                // for each term get tf
                tf_map = tfidf_vectors.get(doc_id);
                if(tf_map == null) tf_map = new HashMap<String, Double>();

                while (vterm!=null)
                {
                    tf=itr.totalTermFreq();
                    // store tf for each term of each document doc_id->(term->tf)
                    tf_map.put(vterm.utf8ToString(),tf);
                    // idf for each unique word
                    if(!idf_vectors.containsKey(vterm.utf8ToString())) {
                        freq = reader.totalTermFreq(new Term("text", vterm.utf8ToString())); //term frequency
                        nidf = Math.log((double) (2*examine) / (double) freq) / Math.log(2*examine);
                        idf_vectors.put(vterm.utf8ToString(), nidf);
                    }

                    vterm=itr.next();
                }
                tfidf_vectors.put(doc_id,tf_map);
                periodic_commmit++;
                if(periodic_commmit>500)
                {
                    periodic_commmit=0;
                    recMngridf.commit();
                    recMngrtfidf.commit();
                }
            }
            recMngridf.commit();
            recMngrtfidf.commit();
            System.out.println("Normalizing tf*idf scores");
            for(Integer d_id:tfidf_vectors.keySet())
            {
                tf_map = tfidf_vectors.get(d_id);
                for(String t:tf_map.keySet())
                {
                    tf=tf_map.get(t);
                    nidf=idf_vectors.get(t);
                    maxtf=Collections.max(tf_map.values());
                    tf_idf = (tf/maxtf) * nidf;
                    tf_map.put(t,tf_idf);
                    periodic_commmit++;

                }
                tfidf_vectors.put(d_id,tf_map);
                if(periodic_commmit>10000)
                {
                    periodic_commmit=0;
                    recMngrtfidf.commit();
                }

            }

            recMngrtfidf.commit();
            recMngrtfidf.close();
            recMngridf.commit();
            recMngridf.close();
            //clean();
            /*
            while(term!=null)
            {
                q = new TermQuery(new Term("text", term.utf8ToString()));
                th = new TotalHitCountCollector();
                searcher.search(q,th);
                docs = searcher.search(q,Math.max(1,th.getTotalHits()));
                // store map of term->idf (for each term obviously)

                //loop over documents of a term
                for(int i=0;i<docs.scoreDocs.length;i++)
                {

                    while
                    tf=term_iter.totalTermFreq();
                    System.out.println(doc_id+" "+tf);
                    // store for each document a map on term->tf
                    HashMap<String,Double> tf_map = tfidf_vectors.get(doc_id);
                    if(tf_map == null) tf_map = new HashMap<String, Double>();
                    tf_map.put(term.utf8ToString(),tf);
                    tfidf_vectors.put(doc_id,tf_map);
                }
                term=term_iter.next();
            }
            for(Integer did:tfidf_vectors.keySet())
            {
                HashMap<String,Double> term_set=tfidf_vectors.get(did);
                double maxtf= Collections.max(term_set.values());
                for(String t: term_set.keySet())
                {
                    double tfidf = term_set.get(t).doubleValue()*idf_vectors.get(t).doubleValue();
                    term_set.put(t,tfidf);
                }
            }
            */

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void clean()
    {
        File toClear=new File("Knn/");
        for(File f:toClear.listFiles())
        {
            f.delete();
        }
    }
}
