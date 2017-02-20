import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.classification.ClassificationResult;
import org.apache.lucene.classification.SimpleNaiveBayesClassifier;
import org.apache.lucene.classification.document.SimpleNaiveBayesDocumentClassifier;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import javax.print.Doc;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by steve on 18/2/2017.
 */
public class NaiveBayes
{
    public static String data_path = "/home/steve/IdeaProjects/ProjectIR/data/train";
    public static int examine=10000;
    public static void main(String[] args)
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
        TermQuery q;TotalHitCountCollector th;TopDocs docs;Document doc;
        //SimpleNaiveBayesClassifier classifier = new SimpleNaiveBayesClassifier();
        HashMap<String,Analyzer> conf=new HashMap<String, Analyzer>();
        conf.put("text",new EnglishAnalyzer());
        Analyzer analyzer = new EnglishAnalyzer();
        //SimpleNaiveBayesDocumentClassifier snv = new SimpleNaiveBayesDocumentClassifier(reader,null,"score",conf);
        SimpleNaiveBayesClassifier snv = new SimpleNaiveBayesClassifier(reader,analyzer,null,"text","text");
        try {
            int count=0,correct=0;
            String[] temp1;
            BufferedReader in;
            ClassificationResult<BytesRef> a;
            File folder = new File(data_path);
            for (File directory : folder.listFiles()) {
                if (directory.isDirectory()) {
                    System.out.println("Analyzing: " + directory.getName());
                    String dir_name = directory.getName();
                    //if(dir_name.equals("pos")) continue;//test negatives directly
                    for (File file : directory.listFiles()) {
                        if (file.isFile() && !file.isHidden() && count < examine) {
                            temp1 = file.getName().split("_");
                            temp1 = temp1[1].split(".txt");
                            //int score = Integer.parseInt(temp1[0]);
                            in = new BufferedReader(new FileReader(data_path + "/" + directory.getName() + "/" + file.getName()));
                            a= snv.assignClass(in.readLine());
                            in=null;
                            //System.out.println(a.getAssignedClass().utf8ToString());
                            //System.out.println(a.getScore()+" "+score);
                            if(dir_name.equals("pos"))
                            {
                                if(a.getScore()>0.5)
                                {
                                    correct++;
                                }
                            }
                            else
                            {
                                if(a.getScore()<0.5)
                                {
                                    correct++;
                                }
                            }
                            count++;
                            System.out.println(count);
                        }
                    }
                }
            }
            System.out.println("Precision: "+(float)correct/(float)count);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error classifing");
        }

    }

}