package Engine.DataAccess;
import Engine.Indexes.Posting;
import java.util.List;

public interface PostingDao {

    List<Posting> readWithoutPositions(String term);

    List<Posting> readWithPositions(String term);

    void write(Posting posting);

}
