package lp;

/**
 * To study some more classification techniques we use LingPipe API provided by
 * alias-i (You can find the link below).
 * To use this API we put the required libraries in the project and use this 
 * code to implement classification on our data.
 * 
 * http://alias-i.com/lingpipe/index.html
 */
import com.aliasi.util.Files;

import com.aliasi.classify.Classification;
import com.aliasi.classify.Classified;
import com.aliasi.classify.DynamicLMClassifier;

import com.aliasi.lm.NGramProcessLM;
import java.io.BufferedWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This class classifies our data with LingPipe API.
 * @author Nikolaos Karampinas & Asterios Chouliaras
 */
public class LingPipeClassification {
    
    File directory;
    String[] categories;
    
    DynamicLMClassifier<NGramProcessLM> aClassifier;
    
    public LingPipeClassification(){
        directory= new File("data/train");
        categories=directory.list();
       
         
        aClassifier= DynamicLMClassifier.createNGramProcess(categories,8);
    }
    
    /**
     * We use this method to train our model with the train set.
    */
    public void train() throws IOException {
        System.out.println("Training data set");
        for (int i=0;i<categories.length;++i){
            String category=categories[i];
            
            Classification classification;
            File file1;
            File[] trainFiles;
                    
            classification= new Classification(category);
            file1=new File(directory,categories[i]);
            trainFiles= file1.listFiles();
            
            for (int j=0;j<trainFiles.length; ++j) {
                File file2= trainFiles[j];
                 // if (isTrainingFile(trainFile)) {
                String review= Files.readFromFile(file2,"ISO-8859-1");
                Classified<CharSequence> classified;
                classified=new Classified<CharSequence>(review,classification);
                aClassifier.handle(classified);
                //}
            }
        }
    }
    
    /**
     * We use this method to test train sets to find our models accuracy.
    */
    
    /*
    boolean isTrainingFile(File file) {
        return file.getName().charAt(2) != '9';  // test on fold 9
    }
    
     public void testTrain() throws IOException{
        int tests=0;
        int corTests=0;
        for (int i=0;i<categories.length;++i){
            String category=categories[i];
            File file1;
            File[] trainFiles;
            
            file1 = new File(directory,categories[i]);
            trainFiles= file1.listFiles();
            for (int j=0;j<trainFiles.length;++j){
                File file2= trainFiles[j];
                 if (!isTrainingFile(file2)) {
                String review = Files.readFromFile(file2,"ISO-8859-1");
                ++tests;
                Classification classification = aClassifier.classify(review);
                if (classification.bestCategory().equals(category))
                        ++corTests;
                 }
            }
        }
        System.out.println("  # Test Cases=" + tests);
        System.out.println("  # Correct=" + corTests);
        System.out.println("  % Correct=" 
                           + ((double)corTests)/(double)tests);
    }
    */
    
    /**
     * We use this method to predict the class of our test set.
     */
    public void test() throws IOException{
        int tests=0;
        int fileClass;
            File file1;
            File[] trainFiles;
            BufferedWriter out = new BufferedWriter(new FileWriter("predictions.txt"));
            
            file1 = new File("data/test");
            trainFiles= file1.listFiles();
            for (int j=0;j<trainFiles.length;++j){
                File file2= trainFiles[j];
                String review = Files.readFromFile(file2,"ISO-8859-1");
                ++tests;
                Classification classification = aClassifier.classify(review);
               // System.out.println("Class: " +classification.bestCategory());
               if (classification.bestCategory().equals("pos")) fileClass=1;
               else fileClass=0;
               String name = file2.getName();
                int pos = name.lastIndexOf(".");
                if (pos > 0) {
                    name = name.substring(0, pos);
                }
                out.write(name+ " " +fileClass);
                out.newLine();
            }
            out.close();
    }
    
    public static void main(String[] args) throws Throwable{
        LingPipeClassification lpc= new LingPipeClassification();
        lpc.train();
       // lpc.testTrain();
        lpc.test();
    }
    
}
