package cecs429.indexes;

import java.util.*;

/**
 * A Posting encapsulates a document ID associated a list of term positions within the document
 */
public class Posting {
	private int mDocumentId;
	private List<Integer> mTermPositions;
	
	public Posting(int documentId, List<Integer> termPositions) {
		mDocumentId = documentId;
		mTermPositions = termPositions;
	}
	
	public int getDocumentId() {
		return mDocumentId;
	}

	public List<Integer> getTermPositions() {
		return mTermPositions;
	}
}
