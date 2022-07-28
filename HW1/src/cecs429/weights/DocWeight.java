package cecs429.weights;


import edu.csulb.Driver.WeighingScheme;
import edu.csulb.Driver.ActiveConfiguration;
import cecs429.indexes.*;
import cecs429.documents.*;
import org.jetbrains.annotations.NotNull;

import static edu.csulb.Driver.ActiveConfiguration.*;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static edu.csulb.Driver.ActiveConfiguration.*;
import static edu.csulb.Driver.RunMode.BUILD;

public class DocWeight implements Weight, Comparable<DocWeight> {
    private double mValue;
    private WeighingStrategy mWeigher;
    private Document mDocument;
    private int mDocId;
    private long mDocLength; // number of TOKENS (NOT TERMS) in doc d
    private long mAvgDocLength;
    private long mByteSize; // size of the document file in bytes
    private int mAvgTermFrequency;
    private List<DocTermWeight> mTermWeights; // list of  w(d,t) values for all the terms in the given doc
    private double mAccumulator;

    public DocWeight(int docId) {
        mValue = 0;
        mDocLength = 0;
        mAvgTermFrequency = 0;
//        mDocument = d;
        mDocId = docId;
        mTermWeights = new ArrayList<>();
//        mByteSize = d.getByteSize();
        mAccumulator = 0;

    }

    public DocWeight(int docId, List<DocTermWeight> termWeights) {
        mValue = 0;
        mDocLength = 0;
        mAvgDocLength = 0;
        mAvgTermFrequency = 0;

        if (termWeights.size() == 0) {
            mAvgTermFrequency = 1;
        }
        else {
            for (DocTermWeight w : termWeights) {
                mAvgTermFrequency += w.getTermFrequency();
            }
            mAvgTermFrequency = mAvgTermFrequency / termWeights.size();
        }

//        mDocument = d;
        mDocId = docId;
        mTermWeights = termWeights;
//        mByteSize = d.getByteSize();
        mAccumulator = 0;
    }

    @Override
    public void calculate(WeighingScheme scheme) {
        mWeigher = scheme.getInstance();
        mValue = mWeigher.calculateLd(this);
    }

    @Override
    public void read(WeighingScheme scheme) {
        mWeigher = scheme.getInstance();
        mValue = mWeigher.readLd(this);
    }

    public double getValue() {
        // if the value hasn't been calculated yet, retrieve it by either calculating it or reading from the disk depening on the active run mode
        if (mValue == 0) {
            if (runMode == BUILD) {
                calculate(activeWeighingScheme);
            }
            else {
                read(activeWeighingScheme);
            }
        }
        return mValue;
    }

    public double readValue() {
        read(activeWeighingScheme);
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

    public int getDocId() {
        return mDocId;
    }

    public List<DocTermWeight> getTermWeights() {
        return mTermWeights;
    }

    public void setTermWeights(List<DocTermWeight> termWeights) {
        this.mTermWeights = termWeights;
    }

    public long getDocLength() {
        return mDocLength;
    }

    public void setDocLength(int mDocLength) {
        this.mDocLength = mDocLength;
    }

    public long getByteSize() {
        return mByteSize;
    }

    public void setByteSize(long byteSize) {
        mByteSize = byteSize;
    }

    public int getAvgTermFrequency() {
        // find the avg tf(t,d) by looping through the map of freq
        if (mAvgTermFrequency == 0) {
            int sumTfTd = 0;
            for (DocTermWeight wDt : mTermWeights) {
                sumTfTd += wDt.getTermFrequency();
            }
            mAvgTermFrequency = Math.floorDiv(sumTfTd, mTermWeights.size());
        }
        return mAvgTermFrequency;
    }

    public void setAvgTermFrequency(int avg) {
        mAvgTermFrequency = avg;
    }

    public void increaseAccumulator(double acc) {
        mAccumulator += acc;
    }

    public double getAccumulator() {
        return mAccumulator;
    }

    public void setAccumulator(double newAcc) {
        mAccumulator = newAcc;
    }

    @Override
    public int compareTo(@NotNull DocWeight w) {
        return Double.compare(mAccumulator, w.getAccumulator());
    }

    public long getAvgDocLength() {
        return mAvgDocLength;
    }


//    @Override
//    public int compareTo(@NotNull DocWeight w) {
//        return Integer.compare(w.getDocument().getId(), mDocument.getId());
//    }
}