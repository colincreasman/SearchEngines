package Engine.Weights;

import App.Driver;
import App.Driver.WeighingScheme;
import Engine.DataAccess.BinFileDao;
import Engine.DataAccess.DbFileDao;

import static App.Driver.ActiveConfiguration.activeWeighingScheme;

// uses default stuff
public abstract class WeighingStrategy {
    public static DbFileDao mDbDao = new DbFileDao(); // used to read the byte location of a requested term
    public static BinFileDao mBinDao = new BinFileDao();
    public static WeighingScheme mScheme;


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
    public double readWdt(DocTermWeight w) {
        mDbDao.open("termLocations");
        String term = w.getTerm();
        int docId = w.getDocument().getId();
        long byteLocation = mDbDao.readTermLocation(term);

        // update byteLocation to offset the read bytes by the ordinal of the passed in weighting scheme
        int schemeOffset = mScheme.ordinal();
        byteLocation += schemeOffset;
        mBinDao.open("postings");



        return 0;
    };
    public double readLd(DocWeight w) {
        double weight = 0.0;
        try {
            mBinDao.open("docWeights");
            int byteLocation = (w.getDocId() * 28); // 28 bytes used up for the 4 values written for each doc
            // the default weighing strategy uses the normal docWeights(d) formula, this should be the first value written for each docId in the file
            weight = mBinDao.readDouble(byteLocation);
            // TODO: delegate the closing of each file to the super calling this
//            mBinDao.close("docWeights");
        }
        catch (Exception ex) {
            System.out.println("Failed to read the doc weight from disk for docId: " + w.getDocId());
            ex.printStackTrace();
        }
        return weight;
    }


}
