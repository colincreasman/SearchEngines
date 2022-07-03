package cecs429.weights;

import cecs429.documents.Document;
import cecs429.weights.WeighingStrategy;
import cecs429.weights.Weight;

import static edu.csulb.Driver.ActiveConfiguration.activeWeigher;

public class DocTermWeight implements Weight {
    private String mTerm;
    private double mValue;
    private WeighingStrategy mWeigher;
    private Document mDocument;
    private int mTermFrequency; // tf(t,d)

    public DocTermWeight() { };

    public DocTermWeight(String term, Document d) {
        mTerm = term;
        mWeigher = activeWeigher;
        mDocument = d;
       // mTermFrequency = termFrequency;
    }

    @Override
    public void calculate() {
        mValue = mWeigher.calculateWdt(this);
    }

    @Override
    public void read() {
        mValue = mWeigher.readWdt(this);
    }

    @Override
    public void write() {
        mWeigher.writeWdt(this);
    };

    public int getTermFrequency() {
        return mTermFrequency;
    }

    public double getValue() {
        return mValue;
    }

    public String getTerm() {
        return mTerm;
    }

    public Document getDocument() {
        return mDocument;
    }

}
