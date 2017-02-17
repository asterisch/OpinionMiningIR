import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.AnalyzerWrapper;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.TypeTokenFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

public class PorterAnalyzer extends AnalyzerWrapper {

    private Analyzer baseAnalyzer;

    public PorterAnalyzer(Analyzer baseAnalyzer) {
        super(new ReuseStrategy() {
            @Override
            public TokenStreamComponents getReusableComponents(Analyzer analyzer, String fieldName) {
                return null;
            }

            @Override
            public void setReusableComponents(Analyzer analyzer, String fieldName, TokenStreamComponents components) {

            }
        });
        this.baseAnalyzer = baseAnalyzer;
    }

    @Override
    public void close() {
        baseAnalyzer.close();
        super.close();
    }

    @Override
    protected Analyzer getWrappedAnalyzer(String fieldName)
    {
        return baseAnalyzer;
    }

    @Override
    protected TokenStreamComponents wrapComponents(String fieldName, TokenStreamComponents components)
    {
        TokenStream ts = components.getTokenStream();
        Set<String> filteredTypes = new HashSet<String>();
        filteredTypes.add("<NUM>");
        TypeTokenFilter numberFilter = new TypeTokenFilter(ts, filteredTypes);

        PorterStemFilter porterStem = new PorterStemFilter(numberFilter);
        return new TokenStreamComponents(components.getTokenizer(), porterStem);
    }

    public static void main(String[] args) throws IOException
    {

        //Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);
        PorterAnalyzer analyzer = new PorterAnalyzer(new StandardAnalyzer());
        String text = "This is a stem testing example port. It should stem-tests the Porter stemmer version 111";

        TokenStream ts = analyzer.tokenStream("fieldName", new StringReader(text));
        ts.reset();

        while (ts.incrementToken()){
            CharTermAttribute ca = ts.getAttribute(CharTermAttribute.class);

            System.out.println(ca.toString());
        }
        analyzer.close();
    }

}