package cecs429.indexes;

import edu.csulb.Driver;
import org.jetbrains.annotations.NotNull;

import static edu.csulb.Driver.ActiveConfiguration.*;
import static edu.csulb.Driver.QueryMode.Ranked;


import java.util.*;
import java.util.Comparator;


/**
 * A Posting encapsulates a document ID associated a list of term positions within the document
 */
public class Posting implements Comparator<Posting> {
	private int mDocumentId;
	private List<Integer> mTermPositions;
	private int mPositionsCount; //same as tf(d)
	private static double mAccumulator;
	// additional field used only in postings for ranked retrievals
	private double mTermWeight; // wdt
	private double mDocWeight; // Ld





	/**
	 * Simple constructor for making a posting with only a documentId at the time of initialization
	 * @param documentId
	 */
	public Posting(int documentId) {
		mDocumentId = documentId;
		mTermPositions = new ArrayList<>();
		mAccumulator = 0;
	}
	/**
	 * Overloaded constructor for making a posting with only a single termPosition at the time of initialization
	 * @param documentId
	 * @param termPosition
	 */
	public Posting(int documentId, int termPosition) {
		mDocumentId = documentId;
		mTermPositions = new ArrayList<>();
		mTermPositions.add(termPosition);
		mAccumulator = 0;

	}
	/**
	 * Overloaded constructor for making a posting with only a a list of termPosition at the time of initialization
	 * @param documentId
	 * @param termPositions
	 */
	public Posting(int documentId, List<Integer> termPositions) {
		mDocumentId = documentId;
		mTermPositions = termPositions;
		//Collections.sort(mTermPositions);
		mPositionsCount = termPositions.size();
		mAccumulator = 0;

	}

	/**
	 * Overloaded constructor for positionless postings in ranked retrieval
	 * includes additional fields for doc/term weights and
	 * @param documentId
	 * @param termWeight
	 * @param termFrequency
	 */
	public Posting(int documentId, double termWeight, int termFrequency) {
		mDocumentId = documentId;
		mTermWeight = termWeight;
		mPositionsCount = termFrequency;
//		mDocWeight = docWeight;
		mAccumulator = 0;
	}
	
	public int getDocumentId() {
		return mDocumentId;
	}

	public List<Integer> getTermPositions() {
		return mTermPositions;
	}


	public void addTermPosition (int position) {
		mTermPositions.add(position);
		Collections.sort(mTermPositions);
	}

	// given two Separate postings objects with the same docID's, returns a single Posting instance that maps their common docId to an aggregate list of  all the term positions from both original Postings (without duplicates)
	public Posting merge(Posting b) {
		///List<Integer> results = new	ArrayList<>();
		// return the original posting if the docId's are not the same
		if  (this.mDocumentId != b.mDocumentId) {
		//	System.out.println("Error: The term positions from the provided postings cannot be merged because they do not map to the same docID. Returning the original (unmerged) Posting\n" );
			return this;
		}
		else {
			// create a HashSet that will store all non-duplicated term positions from both postings
			HashSet<Integer> aggregate = new HashSet<>();
			// add all term positiongs to the set
			aggregate.addAll(this.mTermPositions);
			aggregate.addAll(b.mTermPositions);
			List<Integer> sharedPositions = new ArrayList<>();

			// now convert back into a list to allow Posting instantiation
			sharedPositions.addAll(aggregate);

			Posting results = new Posting(b.mDocumentId, sharedPositions);
			return results;
		}
	}

	// wraps a single posting string as "docId:[pos1,pos2,...,etc.]"
	@Override
	public String toString() {
		String result;
		if (queryMode == Ranked || mTermPositions == null) {
			result = mDocumentId + ": [w(d,t): " + mTermWeight + "; tf(t,d): " + mPositionsCount + "]";
		}
		else {
			result = mDocumentId + ":" + mTermPositions.toString();
		}
		return result;

	}

	public int getPositionsCount() {
		return mPositionsCount;
	}

	public double getTermWeight() {
		return mTermWeight;
	}

	public double getDocWeight() {
		return mDocWeight;
	}

	public void increaseAccumulator(double acc) {
		mAccumulator += acc;
	}

	public double getAccumulator() {
		return mAccumulator;
	}

	@Override
	public int compare(Posting p1, Posting p2) {
		int doc1 = p1.getDocumentId();
		int doc2 = p2.getDocumentId();
		return doc1 - doc2;
	}
}

