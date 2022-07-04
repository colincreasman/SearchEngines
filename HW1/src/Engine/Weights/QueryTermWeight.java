package Engine.Weights;

import Engine.Documents.Document;

import static App.Driver.ActiveConfiguration.activeWeigher;

public class QueryTermWeight implements Weight {
    private String mTerm;
    private double mValue;
    private WeighingStrategy mWeigher;
    private Document mDocument;
    private int mDocFrequency; // Dft

    public QueryTermWeight() { };

    public QueryTermWeight(String term) {
        mWeigher = activeWeigher;
        mTerm = term;

    }

    @Override
    public void calculate() {
        mValue = mWeigher.calculateWqt(this);
    }

    @Override
    public void read() {
        mValue = mWeigher.readWqt(this);
    }

    @Override
    public void write() {
        mWeigher.writeWqt(this);
    };

    public double getValue() {
        return mValue;
    }

    public Document getDocument() {
        return mDocument;
    }

    public String getTerm() {
        return mTerm;
    }

}
