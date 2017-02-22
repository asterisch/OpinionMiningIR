import jdbm.PrimaryTreeMap;
import jdbm.RecordManager;
import jdbm.recman.BaseRecordManager;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by steve on 21/2/2017.
 */
public class Rocchio
{
    private static PrimaryTreeMap<Integer,HashMap<String,Double>> tfidf_vectors;
    private static PrimaryTreeMap<Integer,HashMap<String,Double>> center_vectors;
    private static PrimaryTreeMap<String,Double> idf_vectors;

    private static RecordManager recMngrCenter;
    private static RecordManager recMngrtfidf;
    private static RecordManager recMngridf;

    private static int examine=CreateInverted.examine;
    private static String recNametfidf =KNN.recNametfidf;
    private static String recNameCenter="Rocchio/centers";
    private static String recNameidf="Knn/terms_idf";


    public static void main(String[] args) {
        if (existKnn()) {
            System.out.println("Using Vector Model from Knn");
        } else {
            System.out.println("Training");
            KNN.train();
        }
        System.out.println("Finding Leaders of every class ");
        try {
            centroid();
        } catch (IOException e) {
            e.printStackTrace();
        }
        HashMap<String, Double> centers;
        try {
            recMngrCenter =new BaseRecordManager(recNameCenter);
            center_vectors=recMngrCenter.treeMap(recNameCenter);
            recMngridf=new BaseRecordManager(recNameidf);
            idf_vectors=recMngridf.treeMap(recNameidf);

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        int count=0,correct=0;
        BufferedReader in;
        String line;
        StringBuilder ss;
        String temp1[],doc_id;int score;
        PorterAnalyzer analyzer = new PorterAnalyzer(new EnglishAnalyzer());
        TokenStream ts;
        String term;
        HashMap<String, Double> docQuery;
        File folder = new File(CreateInverted.data_path);
        for (File directory : folder.listFiles()) {
            if (directory.isDirectory()) {
                String dir_name = directory.getName();
                System.out.println("Analyzing: " + directory.getName());
                count = 0;
                for (File file : directory.listFiles()) {
                    if (file.isFile() && !file.isHidden() && count < examine) {
                        try {
                            temp1 = file.getName().split("_");
                            doc_id = temp1[0];
                            temp1 = temp1[1].split(".txt");
                            score = Integer.parseInt(temp1[0]);
                            in = new BufferedReader(new FileReader(CreateInverted.data_path + "/" + directory.getName() + "/" + file.getName()));
                            ss = new StringBuilder();
                            docQuery = new HashMap<String, Double>();

                            while ((line = in.readLine()) != null) {
                                ss.append(line);
                            }
                            ts = analyzer.tokenStream("fieldName", new StringReader(ss.toString()));
                            ts.reset();
                            while (ts.incrementToken()) {
                                CharTermAttribute ca = ts.getAttribute(CharTermAttribute.class);
                                term = ca.toString();
                                if (docQuery.containsKey(term)) {
                                    double tf = docQuery.get(term).doubleValue() + 1;
                                    docQuery.put(term, tf);
                                } else {
                                    docQuery.put(term, 1.0);
                                }
                            }
                            double maxtf = Collections.max(docQuery.values());
                            // Calculate query tf idf scores (Salton & Buckley method)
                            for (String trm : docQuery.keySet()) {
                                double index_idf = idf_vectors.get(trm).doubleValue();
                                double qIdf = ((docQuery.get(trm) / maxtf)) * index_idf;
                                docQuery.put(trm, qIdf);
                            }
                            double maxsim=0;int to_class=0;
                            for (int i = 0; i < 10; i++) {
                                centers = center_vectors.get(i);
                                if(centers==null) continue;
                                double dot=0,Qnorm=0,Dnorm=0,sim;
                                for (String qterm : docQuery.keySet()) {
                                    if(centers.containsKey(qterm))
                                    {
                                        dot+=centers.get(qterm)*docQuery.get(qterm);
                                        Qnorm+=Math.pow(docQuery.get(qterm),2);
                                        Dnorm+=Math.pow(centers.get(qterm),2);
                                    }
                                }
                                sim=dot / (Math.sqrt(Qnorm)*Math.sqrt(Dnorm));
                                if(sim>maxsim)
                                {
                                    maxsim=sim;
                                    to_class=i;
                                }
                            }
                            if(score>5)
                            {
                                if(to_class>5) correct++;
                            }
                            else
                            {
                                if(to_class<5) correct++;
                            }
                            ss.delete(0, ss.length());
                            ss = null;

                        } catch (IOException e) {

                        }
                        count++;
                    }
                }
            }
        }
        System.out.println(correct+" "+(2*count));
        System.out.println("Precision: "+(double)correct/(double)(2*count));
        try {
            recMngrCenter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private static void centroid() throws IOException {
        clean();
        recMngrtfidf= new BaseRecordManager(recNametfidf);
        recMngrCenter=new BaseRecordManager(recNameCenter);
        tfidf_vectors = recMngrtfidf.treeMap(recNametfidf);
        center_vectors=recMngrCenter.treeMap(recNameCenter);

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
            //System.out.println(centers);
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