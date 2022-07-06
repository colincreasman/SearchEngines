package Engine.DataAccess;

import java.io.File;
import java.io.FileNotFoundException;

public class TxtFileDao extends FileDao {

    public TxtFileDao() {
        super();
        mFileExt = ".txt";
    }

    public TxtFileDao(File sourceDir) {
        super(sourceDir);
        mFileExt = ".txt";
    }


    @Override
    public void create(String filePath) {
    }

    @Override
    public void open(String name) {

    }

    @Override
    public void close(String name) {

    }
}


