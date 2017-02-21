import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * Created by steve on 16/2/2017.
 */
public class CreateInverted
{
    public static String index_path="/tmp/inverted_index";
    public static String data_path = "/home/steve/IdeaProjects/ProjectIR/data/train";
    public static int examine = 10000;
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
        Analyzer analyzer = new PorterAnalyzer(new EnglishAnalyzer());

        // Store the index in memory:
        // Directory dir = FSDirectory.open(Paths.get("/home/steve/IdeaProjects/ProjectIR/data"));
        // To store an index on disk, use this instead:
        Directory dir = null;
        try {
            dir = FSDirectory.open(Paths.get(index_path));
        } catch (IOException e) {
            System.err.println("[+] Error writing CreateInverted Index to "+index_path);
        }
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter iwriter = null;
        try {
            iwriter = new IndexWriter(dir, config);


        Document doc;

        BufferedReader in;
        File folder = new File(data_path);
            for (File directory : folder.listFiles()) {
                if (directory.isDirectory()) {
                    System.out.println("Analyzing: " + directory.getName());
                    for (File file : directory.listFiles()) {
                        if (file.isFile() && !file.isHidden() && count < examine) {
                            doc = new Document();
                            temp1 = file.getName().split("_");
                            doc_id = temp1[0];
                            temp1 = temp1[1].split(".txt");
                            score = Integer.parseInt(temp1[0]);
                            in = new BufferedReader(new FileReader(data_path + "/" + directory.getName() + "/" + file.getName()));
                           // System.out.println(doc_id+" "+score);
                            FieldType type = new FieldType();
                            type.setStoreTermVectors(true);
                            type.setStored(true);
                            type.setTokenized(true);
                            type.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
                            doc.add(new Field("ID", doc_id, TextField.TYPE_STORED));
                            doc.add(new Field("text",in.readLine(),type));
                            //doc.add(new Field("text", in.readLine(), TextField.TYPE_NOT_STORED));
                            doc.add(new Field("score", String.valueOf(score), TextField.TYPE_STORED));

                            iwriter.addDocument(doc);
                            doc = null;
                            in = null;
                            count++;
                        }


                    }
                }
            }
            iwriter.commit();
            iwriter.close();
        }catch (IOException e)
        {
            System.err.println("[-] Error writing documents to inverted index");
        }

    }
}
