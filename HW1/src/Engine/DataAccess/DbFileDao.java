package Engine.DataAccess;

import Engine.Indexes.Posting;

import java.io.File;
import java.util.List;

public class DbFileDao extends FileDao {


    public DbFileDao(String filePath) {
        super(filePath);
    }

    @Override
    public boolean connect(File sourceFile) {
        return false;
    }

}
