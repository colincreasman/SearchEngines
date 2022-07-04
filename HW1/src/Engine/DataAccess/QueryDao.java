package Engine.DataAccess;

import Engine.Indexes.Posting;
import Engine.Queries.QueryComponent;

import javax.management.Query;
import java.io.File;
import java.util.List;

public interface QueryDao  {

    QueryComponent read(int queryId);

    void write(QueryComponent query);

}

