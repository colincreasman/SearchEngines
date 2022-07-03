package cecs429.weights;

// uses default stuff
public abstract class WeighingStrategy {


    public abstract double calculateWdt(DocTermWeight w);
    public abstract double calculateWqt(QueryTermWeight w);
    public abstract double calculateLd(DocWeight w);

    // the process for reading and writing weights should be the same regardless of the strategy being implemented, so they are defined here as non-abstract methods to be inherited by each concrete strategy implmentation.
    // TODO: define these to accurately read/write weights
    public double readWdt(DocTermWeight w) {
        return 0;
    }

    public double readWqt(QueryTermWeight w) {
        return 0;
    }

    public double readLd(DocWeight w) {
        return 0;
    }

    public double writeWdt(DocTermWeight w) {
        return 0;
    }

    public double writeWqt(QueryTermWeight w) {
        return 0;
    }

    public double writeLd(DocWeight w) {
        return 0;
    }

}
