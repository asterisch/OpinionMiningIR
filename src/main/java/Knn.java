import com.sun.org.apache.regexp.internal.RE;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Need to find similarities to use the method correctly
 * Created by nikos on 2/19/17.
 */
public class Knn {
    public static void main(String[] args)
    {
        int K = 5;

        HashMap<Review,HashSet<HashMap<Review,Double>>> similarities;
        similarities= new HashMap<Review,HashSet<HashMap<Review,Double>>>();

        /*
            Fake similarities data
         */
        for (int j=0;j<20;j++){
            double x=0.0;
            HashMap<Review,Double> fake1=new HashMap<Review, Double>();
            Review rFake=new Review(0,j);
            Review rFake2;
            for (int b=0;b<20;b++) {
                if (b%2==0) //zugo
                rFake2=new Review(0,b);
                else  rFake2=new Review(1,b);
                fake1.put(rFake2, (x/20.0));
                x+=1;
            }

            HashSet<HashMap<Review,Double>> fake2=new HashSet<HashMap<Review,Double>>();
            fake2.add(fake1);

            similarities.put(rFake,fake2);

        }

        //Needs similarities hashMap
        ArrayList<Review> results;

        KnnImplementation knn=new KnnImplementation(similarities);
        knn.findNeighbors();
        results=knn.findClass();

        Collections.sort(results, new Comparator<Review>() {
            @Override
            public int compare(Review review2, Review review1)
            {

                return  review2.get_id() - (review1.get_id());
            }
        });

        try {
            BufferedWriter out = new BufferedWriter(new FileWriter("predictions.txt"));
            for (Review r:results) {
                //Print to out file
                System.out.println("Doc's ID=" + r.get_id() +" Doc's Class=" +r.get_class());
                out.write(r.get_id()+ " " + r.get_class());
                out.newLine();
            }
            out.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}
