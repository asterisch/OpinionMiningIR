import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Needs one HashMap with Review(A doc on test) with many pairs of other docs and their similarities
 * A doc is a Review class with it's id and it's classification
 * Created by nikos on 2/18/17.
 */
public class KnnImplementation {

    /*
        Using these maps,lists until queries with lucene are made
     */

    private ArrayList<Review> neighbors;
    private ArrayList<Review> results;
    private HashMap<Review,ArrayList<Review>> neighborsMap;
    private ArrayList<Double> maxKSimilarities;
    private HashMap<Review,HashSet<HashMap<Review,Double>>> similarities; //Integer=similarity
    private final int K = 5;

    private int positives;
    private int negatives;


    public KnnImplementation(HashMap<Review,HashSet<HashMap<Review,Double>>> sim) {

        neighbors = new ArrayList<Review>(K);
        results = new ArrayList<Review>(K);
        neighborsMap= new HashMap<Review,ArrayList<Review>>();
        maxKSimilarities= new ArrayList<Double>(K);
        for (int i=0;i<K;i++) {
            maxKSimilarities.add((double) 0);
        }
        similarities= new HashMap<Review, HashSet<HashMap<Review,Double>>>(sim);
    }

    public void findNeighbors() {
            for (Map.Entry<Review, HashSet<HashMap<Review,Double>>> entry : similarities.entrySet()) {
                Review key = entry.getKey();
                HashSet<HashMap<Review,Double>> value = entry.getValue();
                for (HashMap<Review,Double> r : value) {
                    for (Map.Entry<Review,Double> s:r.entrySet()) {
                        Review key2 = s.getKey();
                        Double value2 = s.getValue();
                        for (int i=0;i<K;i++) {
                            if (maxKSimilarities.get(i) <value2) {
                                neighbors.add(key2);
                                //System.out.println("Doc's ID=" + key.get_id() +" neighbor's ID=" +key2.get_id());
                                maxKSimilarities.add(i,value2);
                                break;
                            }
                    }
                    neighborsMap.put(key,neighbors);
                    }
            }
        }
    }

    /**
     * Creaters an ArrayList with Review(is a doc) where every Review has it's Doc_ID and it's new Class
     * with KNN implementation
     * @return an ArrayList
     */
    public ArrayList<Review> findClass() {
        for (Map.Entry<Review, ArrayList<Review>> entry : neighborsMap.entrySet()) {
            int newClass,i=0;
            positives = 0;
            negatives = 0;
            Review key = entry.getKey();
            ArrayList<Review> value = entry.getValue();
            for (Review r : value) {
                if (r.get_class() == 1) //1 for positive
                    positives++;
                else
                    negatives++;
            }
            if (positives>negatives) newClass=1;
            else newClass=0;
            Review r1=new Review(newClass,key.get_id());
            results.add(i,r1);
            i++;
        }
        return results;
    }

}
