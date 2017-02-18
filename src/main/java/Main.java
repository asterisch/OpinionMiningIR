
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.Query.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

import java.io.*;
import java.nio.file.Paths;
import java.util.HashSet;


public class Main {

    public static String index_path="/tmp/inverted_index";
    public static final String data_path = "/home/steve/IdeaProjects/ProjectIR/data/train";

    public static void main(String[] args) throws IOException {

        Directory dir = null;
        try {
            dir = FSDirectory.open(Paths.get(index_path));
        } catch (IOException e) {
            System.err.println("[+] Error writing CreateInverted Index to "+index_path);
        }
        Analyzer analyzer = new StandardAnalyzer();
        DirectoryReader ireader = DirectoryReader.open(dir);
        IndexSearcher isearcher = new IndexSearcher(ireader);
        // Parse a simple query that searches for "text":
        QueryParser parser = new QueryParser("text", analyzer);
        Query query = null;
        try {
            query = parser.parse("9");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        ScoreDoc[] hits = isearcher.search(query,2,Sort.RELEVANCE).scoreDocs;
        // Iterate through the results:
       // System.out.println(hits[hits.length-1]);
        dir.close();
        //DirectoryReader reader = DirectoryReader.open(dir);

        //get docs from term
        IndexSearcher searcher = new IndexSearcher(ireader);
        //TermQuery query = new TermQuery(new Term("field", "term"));
        //TopDocs topdocs = searcher.query(query, numberToReturn);

        //get terms
        Fields fields = MultiFields.getFields(ireader);
        Terms terms = fields.terms("text");
        TermsEnum termsEnum = terms.iterator();
        BytesRef t = termsEnum.next();
        HashSet<Integer> scores;
        TermQuery q;TotalHitCountCollector th;TopDocs docs;Document doc;
        while (t != null) {
            scores = new HashSet<Integer>();
            q = new TermQuery(new Term("text", t.utf8ToString()));
            th = new TotalHitCountCollector();
            searcher.search(q,th);
            docs = searcher.search(q,Math.max(1,th.getTotalHits()));
            System.out.printf(t.utf8ToString()+" docs:");
            for(int i=0;i<docs.scoreDocs.length;i++)
            {
                int id=docs.scoreDocs[i].doc;
                Document rdoc = searcher.doc(id);
                scores.add(Integer.valueOf(rdoc.get("ID")));
            }
            System.out.println(scores);
            System.out.printf("\n");
            scores=null;
            t=termsEnum.next();
        }
        ireader.close();

    }
}