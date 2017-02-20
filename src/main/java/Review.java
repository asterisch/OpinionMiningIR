/**
 * Created by nikos on 2/18/17.
 */
public class Review {

    private int r_class;
    private int doc_id;

    public Review(int aR_class, int aDoc_id) {
        r_class=aR_class;
        doc_id=aDoc_id;
    }

    public int get_class(){
        return r_class;
    }

    public int get_id() {
        return doc_id;
    }
}
