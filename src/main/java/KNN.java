import jdbm.PrimaryTreeMap;
import jdbm.RecordManager;
import jdbm.recman.BaseRecordManager;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by steve on 21/2/2017.
 */
public class KNN
{

    private static String recNametfidf ="Knn/tfidf_vectors";
    private static String recNameidf="Knn/terms_idf";
    private static int examine=10000;
    private static RecordManager recMngridf;
    private static RecordManager recMngrtfidf;
    private static PrimaryTreeMap<Integer,HashMap<String,Double>> tfidf_vectors;
    private static PrimaryTreeMap<String,Double> idf_vectors;

    public static void main(String[] args)
    {
        train();
    }
    private static void train()
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
        // Get terms from lucene's Inverted Index
        Terms terms = null;
        try {
            terms = MultiFields.getTerms(reader,"text");
            TermsEnum term_iter= terms.iterator();
            BytesRef term = term_iter.next();
            double max_tf=0,nidf,tf,ntf,tf_idf;
            long freq,doc_size;
            int doc_id;
            int knn_class,periodic_commmit=0;
            HashMap<String,Double> tf_map;
            term_iter=terms.iterator();
            term=term_iter.next();
            // query all docs
            q = new MatchAllDocsQuery();
            docs = searcher.search(q,reader.numDocs());
            // For each document
            for(int i=0;i<docs.scoreDocs.length;i++)
            {
                doc_id = docs.scoreDocs[i].doc;
                // get term vectors
                Terms termsVector = reader.getTermVector(doc_id,"text");
                if(termsVector==null) continue;
                TermsEnum itr=termsVector.iterator();
                BytesRef vterm=itr.next();
                // for each term get tf
                while (vterm!=null)
                {
                    tf=itr.totalTermFreq();
                    // store tf for each term of each document doc_id->(term->tf)
                    tf_map = tfidf_vectors.get(doc_id);
                    if(tf_map == null) tf_map = new HashMap<String, Double>();
                    tf_map.put(term.utf8ToString(),tf);
                    tfidf_vectors.put(doc_id,tf_map);
                    // idf for each unique word
                    if(!idf_vectors.containsKey(vterm.utf8ToString())) {
                        freq = reader.totalTermFreq(new Term("text", vterm.utf8ToString())); //term frequency
                        nidf = Math.log((double) examine / freq) / Math.log(examine);
                        idf_vectors.put(term.utf8ToString(), nidf);
                    }

                    vterm=itr.next();
                }
                periodic_commmit++;
                if(periodic_commmit>300)
                {
                    periodic_commmit=0;
                    recMngridf.commit();
                    recMngrtfidf.commit();
                }
            }

            for(Integer d_id:tfidf_vectors.keySet())
            {
                tf_map = tfidf_vectors.get(d_id);
                for(String t:tf_map.keySet())
                {
                    tf=tf_map.get(t);
                    nidf=idf_vectors.get(t);
                    double maxtf=Collections.max(tf_map.values());
                    tf_idf = (tf/max_tf) * nidf;
                    tf_map.put(t,tf_idf);
                    periodic_commmit++;
                }
                tfidf_vectors.put(d_id,tf_map);
                System.out.println(d_id+" "+tf_map);
                if(periodic_commmit>500)
                {
                    periodic_commmit=0;
                    recMngrtfidf.commit();
                }

            }

            recMngrtfidf.commit();
            recMngrtfidf.close();
            recMngridf.commit();
            recMngridf.close();
            clean();
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