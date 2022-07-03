package cecs429.weights;

import cecs429.documents.Document;

import java.util.List;

import static edu.csulb.Driver.ActiveConfiguration.activeWeigher;

public class DocWeight implements Weight {
    private double mValue;
    private WeighingStrategy mWeigher;
    private Document mDocument;
    private List<DocTermWeight> mDocTermWeights; // list of  w(d,t) values for all the terms in the given doc

    public DocWeight() {};

    public DocWeight(Document d, List<DocTermWeight> docTermWeights) {
        mWeigher = activeWeigher;
        mDocument = d;
        mDocTermWeights = docTermWeights;
    }

    @Override
    public void calculate() {
        mValue = mWeigher.calculateLd(this);
    }

    @Override
    public void read() {
        mValue = mWeigher.readLd(this);
    }

    @Override
    public void write() {
        mWeigher.writeLd(this);
    }


    public double getValue() {
        return mValue;
    }

    public void setValue(double mValue) {
        this.mValue = mValue;
    }

    public Document getDocument() {
        return mDocument;
    }

    public void setDocument(Document mDocument) {
        this.mDocument = mDocument;
    }

    public List<DocTermWeight> getTermFrequencies() {
        return mDocTermWeights;
    }

    public void setTermFrequencies(List<DocTermWeight> docTermWeights) {
        this.mDocTermWeights = mDocTermWeights;
    }
}
