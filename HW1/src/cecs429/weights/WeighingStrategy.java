package cecs429.weights;


import static edu.csulb.Driver.ActiveConfiguration.*;
import static edu.csulb.Driver.WeighingScheme;
import cecs429.indexes.Index;
import cecs429.indexes.Posting;
import cecs429.text.TokenProcessor;

// uses default stuff
public abstract class WeighingStrategy {
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
    public double readWdt(DocTermWeight w) {
        double wDt = 0.0;
        long byteLocation = indexDao.readByteLocation(w.getTerm());
        // offset by (12 * docId + 4) to get to the weights of each term in the postings.
        int weightIncrement = 4 + mScheme.ordinal();
        // the default weight is written first, so there is no additional offset from the scheme
        byteLocation += weightIncrement;
        wDt = indexDao.readDocTermWeight(byteLocation);
        return wDt;
    }
    public double readLd(DocWeight w) {
        double weight = 0.0;
        int byteLocation = (w.getDocId() * 28); // 28 bytes used up for the 4 values written for each doc
        // the default weighing strategy uses the normal docWeights(d) formula, this should be the first value written for each docId in the file
        weight = indexDao.readDocWeight(byteLocation);

        return weight;
    }


}