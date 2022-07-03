package cecs429.weights;

import cecs429.documents.Document;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static edu.csulb.Driver.ActiveConfiguration.activeWeigher;

public class DocWeight implements Weight, Comparable<DocWeight> {
    private double mValue;
    private WeighingStrategy mWeigher;
    private Document mDocument;
    private int mDocLength; // number of TOKENS (NOT TERMS) in doc d
    private int mByteSize; // size of the document file in bytes
    private HashMap<String, Integer> mTermFrequencies;
    private int mAvgTermFrequency;



    private List<DocTermWeight> mTermWeights; // list of  w(d,t) values for all the terms in the given doc

    public DocWeight() {}

    public DocWeight(Document d, HashMap<String, Integer> termFrequencies) {
        mWeigher = activeWeigher;
        mDocument = d;
        mTermWeights = new ArrayList<>();
        mTermFrequencies = termFrequencies;
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
        return mTermWeights;
    }

    public void setTermFrequencies(List<DocTermWeight> termWeights) {
        this.mTermWeights = termWeights;
    }

    public int getDocLength() {
        return mDocLength;
    }

    public void setDocLength(int mDocLength) {
        this.mDocLength = mDocLength;
    }

    public int getByteSize() {
        return mByteSize;
    }

    public int getAvgTermFrequency() {
        return mAvgTermFrequency;
    }

    @Override
    public int compareTo(@NotNull DocWeight w) {
        return Integer.compare(w.getDocument().getId(), mDocument.getId());
    }
}
