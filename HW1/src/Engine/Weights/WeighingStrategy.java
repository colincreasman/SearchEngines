package Engine.Weights;

import App.Driver;
import App.Driver.WeighingScheme;
import Engine.DataAccess.BinFileDao;
import Engine.DataAccess.DbFileDao;

import static App.Driver.ActiveConfiguration.activeWeighingScheme;

// uses default stuff
public abstract class WeighingStrategy {
    private static DbFileDao mDbDao = new DbFileDao(); // used to read the byte location of a requested term
    private static BinFileDao mBinDao = new BinFileDao();
    private static WeighingScheme mScheme;


    // default constructor instantiates whatever implementation is indicated by the global weight scheme
    public WeighingStrategy getWeigher() {
        return activeWeighingScheme.getInstance();
    }

    // acts as a factory constructor to instantiate the appropriate implementation based on the enum type passed in
    public WeighingStrategy getWeigher(WeighingScheme scheme) {
        mScheme = scheme;
        return scheme.getInstance();
    }


    public abstract double calculateWdt(DocTermWeight w);
    public abstract double calculateWqt(QueryTermWeight w);
    public abstract double calculateLd(DocWeight w);

    // the process for reading and writing weights should be the same regardless of the strategy being implemented, so they are defined here as non-abstract methods to be inherited by each concrete strategy implementation.
    // TODO: define these to accurately read/write weights
    public abstract double readWdt(DocTermWeight w);
//        mDbDao.open("termLocations");
//        String term = w.getTerm();
//        int docId = w.getDocument().getId();
//        long byteLocation = mDbDao.readTermLocation(term);
//
//        // update byteLocation to offset the read bytes by the ordinal of the passed in weighting scheme
//        int schemeOffset = mScheme.ordinal();
//        byteLocation += schemeOffset;
//        mBinDao.open("postings");
//
//        return 0;
//    };
    public abstract double readLd(DocWeight w);

}
