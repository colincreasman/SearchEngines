package Engine.Weights;

import Engine.DataAccess.DbFileDao;
import Engine.DataAccess.FileDao;


// uses default stuff
public abstract class WeighingStrategy {
//    private FileDao weightReader = new DbFileDao(); // used to read the byte locations of terms beind read by each strategy

    public abstract double calculateWdt(DocTermWeight w);
    public abstract double calculateWqt(QueryTermWeight w);
    public abstract double calculateLd(DocWeight w);

    // the process for reading and writing weights should be the same regardless of the strategy being implemented, so they are defined here as non-abstract methods to be inherited by each concrete strategy implementation.
    // TODO: define these to accurately read/write weights
    public abstract double readWdt(DocTermWeight w);
    public abstract double readWqt(QueryTermWeight w);
    public abstract double readLd(DocWeight w);


}
