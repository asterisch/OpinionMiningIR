import jdbm.PrimaryTreeMap;
import jdbm.RecordManager;
import jdbm.recman.BaseRecordManager;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by steve on 21/2/2017.
 */
public class Rocchio
{
    private static PrimaryTreeMap<Integer,HashMap<String,Double>> tfidf_vectors;
    private static PrimaryTreeMap<Integer,HashMap<String,Double>> center_vectors;
    private static RecordManager recMngrCenter;
    private static RecordManager recMngrtfidf;
    private static int examine=CreateInverted.examine;
    private static String recNametfidf =KNN.recNametfidf;
    private static String recNameCenter="Rocchio/centers";

    public static void main(String[] args) {
        if(existKnn())
        {
            System.out.println("Using Vector Model from Knn");
        }
        else
        {
            System.out.println("Training");
            KNN.train();
        }
        /*System.out.println("Finding Leaders of every class ");
        try {
            centroid();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        int count;
        File folder = new File(CreateInverted.data_path);
        for (File directory : folder.listFiles()) {
            if (directory.isDirectory()) {
                String dir_name = directory.getName();
                System.out.println("Analyzing: " + directory.getName());
                count = 0;
                for (File file : directory.listFiles()) {
                    if (file.isFile() && !file.isHidden() && count < examine) {
                        count++;
                    }

                }
            }
        }

    }
    private static void centroid() throws IOException {
        clean();
        recMngrtfidf= new BaseRecordManager(recNametfidf);
        recMngrCenter=new BaseRecordManager(recNameCenter);
        tfidf_vectors = recMngrtfidf.treeMap(recNametfidf);
        center_vectors=recMngrCenter.treeMap(recNameCenter);
        String term_idf_name="Rocchio/temp";
        PrimaryTreeMap<String,Double> term_idf;
        RecordManager term_idf_recMngr=new BaseRecordManager(term_idf_name);
        term_idf=term_idf_recMngr.treeMap(term_idf_name);
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

        HashMap<String,Double> tfidf_map = null,centers=null;
        int doc_class;int[] docs_in_class=new int[10];int periodic_commit=0;
        int count =0;
        for(Integer i : tfidf_vectors.keySet())
        {
            doc_class=Integer.valueOf(reader.document(i).get("score"))-1;

            docs_in_class[doc_class]++;
            tfidf_map=tfidf_vectors.get(i);
            centers=center_vectors.get(doc_class);
            if(centers==null) centers=new HashMap<String, Double>();
            for(String term : tfidf_map.keySet())
            {
                if(centers.containsKey(term))
                {
                    double center_tfidf =centers.get(term);
                    double index_tfidf=tfidf_map.get(term);
                    centers.put(term,center_tfidf+index_tfidf);
                }
                else
                {
                    centers.put(term,tfidf_map.get(term));
                }

                periodic_commit++;
            }
            center_vectors.put(doc_class,centers);
            if(periodic_commit>10000)
            {
                periodic_commit=0;
                recMngrCenter.commit();
            }
            count++;
        }
        recMngrCenter.commit();
        System.out.println("Normalizing centers");
        for(int i=0;i<10;i++) { //Normalize
            centers=center_vectors.get(i);
            if(centers==null)continue;
            for (Map.Entry<String, Double> entry : centers.entrySet()) {
                entry.setValue(entry.getValue()/(double) docs_in_class[i]);
            }
            center_vectors.put(i,centers);
            System.out.println(centers);
        }

        reader.close();
        dir.close();

        recMngrCenter.commit();
        recMngrCenter.close();

        recMngrtfidf.close();

    }
    private static boolean existKnn()
    {
        File toCheck=new File("Knn/");
        if(!toCheck.exists()) return false;
        int i=0;
        for(File f : toCheck.listFiles())
        {
            i+=f.isFile()?1:0;
        }
        return i>0?true:false;

    }
    private static void clean()
    {
        File toClear=new File ("Rocchio/");
        for(File f : toClear.listFiles())
        {
            f.delete();
        }
    }
}