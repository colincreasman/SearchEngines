package Engine.DataAccess;

import Engine.Indexes.Posting;
import Engine.Queries.QueryComponent;
import Engine.Weights.Weight;


import java.io.File;
import java.util.List;

public abstract class FileDao implements WeightDao, PostingDao, QueryDao {
    private File mFile;

    public FileDao(String filePath) {
        mFile = create(filePath);
    }

    public File create(String filePath) {
        return new File(filePath);
    }

    public abstract boolean connect(File sourceFile);


    //TODO: figure out which ones of these to make abstract, then implement DbFileDao, TxtFileDao, and BinFileDao

    @Override
    public List<Posting> readWithoutPositions(String term) {
        return null;
    }

    @Override
    public List<Posting> readWithPositions(String term) {
        return null;
    }

    @Override
    public void write(Posting posting) {

    }

    @Override
    public QueryComponent read(int queryId) {
        return null;
    }

    @Override
    public void write(QueryComponent query) {

    }

    @Override
    public Weight read(long byteLocation) {
        return null;
    }

    @Override
    public void write(Weight weight) {

    }
}
