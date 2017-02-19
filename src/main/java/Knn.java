import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

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
            Review rFake2=new Review(0,0);
           //System.out.println("Doc's ID=" + rFake.get_id());
            for (int b=0;b<20;b++) {

                rFake2=new Review(0,b);
                fake1.put(rFake2, (x/20.0));
               //System.out.println("neighbors's ID=" + rFake2.get_id()+" sim="+( x/20.0));
                x+=1;
            }

            HashSet<HashMap<Review,Double>> fake2=new HashSet<HashMap<Review,Double>>();
            fake2.add(fake1);

            similarities.put(rFake,fake2);

           // System.out.println();
        }

        ArrayList<Review> results;
        //results= new ArrayList<Review>(K);

        KnnImplementation knn=new KnnImplementation(similarities);
        knn.findNeighbors();
        results=knn.findClass();
        for (Review r:results) {
            //Print to out file
            System.out.println("Doc's ID=" + r.get_id() +" Doc's Class=" +r.get_class());
        }
    }
}
