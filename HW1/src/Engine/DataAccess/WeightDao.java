package Engine.DataAccess;

import Engine.Weights.Weight;

public interface WeightDao {

    Weight read(long byteLocation);

    void write(Weight weight);

}
