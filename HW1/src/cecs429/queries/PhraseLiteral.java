package cecs429.queries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import cecs429.indexes.Index;
import cecs429.indexes.Posting;
import cecs429.text.AdvancedTokenProcessor;

/**
 * Represents a phrase literal consisting of one or more terms that must occur in sequence.
 */
public class PhraseLiteral implements QueryComponent {
	// The list of individual terms in the phrase.
	private List<String> mTerms;
	private AdvancedTokenProcessor mProcessor;

	/**
	 * Constructs a PhraseLiteral with the given individual phrase terms.
	 */
	public PhraseLiteral(List<String> terms) {
		mProcessor = new AdvancedTokenProcessor();
		mTerms = new ArrayList<>();
		// before getting postings, each term in the list must be processed (without removing hyphens)
		for (String t : terms) {
			String processedTerm = mProcessor.processTokenWithHyphens(t);
			mTerms.add(processedTerm);
		}
	}
	
	/**
	 * Constructs a PhraseLiteral given a string with one or more individual terms separated by spaces.
	 */
	public PhraseLiteral(String terms) {
		mTerms.addAll(Arrays.asList(terms.split(" ")));
	}
	
	@Override
	public List<Posting> getPostings(Index index) {
		// initialize master list to hold all of the postings that pass each positional merge
		List<Posting> masterList = index.getPostings(mTerms.get(0));

		// loop through the remaining terms in the phrase and perform the positionalMerge
		for (int i = 1; i < mTerms.size(); i++) {
			// update the master list by positionally merging it with the current postings list
			List<Posting> currentPostings = index.getPostings(mTerms.get(i));
				masterList = positionalMerge(masterList, currentPostings);
		}
//		try {
//			return masterList;
//		}
//		catch (NullPointerException ex) {
//			System.out.println("No documents were found containing the phrase literal " + this );
//			return null;
//		}
		if (masterList == null) {
			System.out.println("No documents were found containing the phrase literal " + this );
			return null;
		}
		else {
			return masterList;
		}
	}

	/**
	 * Given two postings lists that each corresponds to a term (or group of terms) from the list of mTerms, finds the postings of documents that contain both of them together in order
	 * @param top represents the master "top"-level that holds all the postings of terms that have already been positionally merged together
	 * @param bottom represents the new "bottom"-level list of postings for the new term that is being merged with the master
	 * @return a new list containing only the postings that survived the merge
	 */
	public List<Posting> positionalMerge(List<Posting> top, List<Posting> bottom) {
		// initialize results list
		List<Posting> mergedPostings = new ArrayList<>();
		// setup indexes for iterating through the postings list of both top and bottom lists
		int topPostingIndex = 0;
		int bottomPostingIndex = 0;

		// starting with the 0th index of each list, iterate through their postings lists simultaneously
		while (topPostingIndex < top.size() && bottomPostingIndex < bottom.size()) {

			// check if the current docIds match before proceeding
			if (top.get(topPostingIndex).getDocumentId() == bottom.get(bottomPostingIndex).getDocumentId()) {
				// lists to store the term positions at the current index in the top and bottom lists
				List<Integer> topTermPositions = top.get(topPostingIndex).getTermPositions();
				List<Integer> bottomTermPositions = bottom.get(bottomPostingIndex).getTermPositions();

				// create lists of matching term locations for each original list
				List<Integer> topMatches = new ArrayList<>();
				List<Integer> bottomMatches = new ArrayList<>();

				// starting at current top-level index of each list, iterate through their lists of the term positions using new indexes
				int topTermIndex = 0;
				int bottomTermIndex = 0;
				while (topTermIndex < topTermPositions.size() && bottomTermIndex < bottomTermPositions.size()) {
					 // start at 1 aboe the top index because it is impossible for a matching to occur at the 0th index of the bottom (this would require the matching in the top to occur at the -1st index)
					int currentBottom = bottomTermPositions.get(bottomTermIndex); // the actual term location found at a given index in the bottom list
					int currentTop = topTermPositions.get(topTermIndex); // the actual term location found at a given index in the top list
					int bottomMargin = currentBottom - currentTop; // the numerical difference between the current values of the top and bottom's term locations

					// success case occurs when the currentBottom location is one number higher than the currentTop location
					if (bottomMargin == 1) {
						// however, in order to truly be a successful matching, the term location immediately preceding currentBottom must also equal the original (un-incremented) currentTop location
						if (bottomTermPositions.get(bottomTermIndex - 1) == currentTop) {
							// its possible to have many pairs of matching term positions among the two lists
							// so for each pair of matches, store the location from the top list into its own list of matches and the location from the bottom into a separate list of matches
							topMatches.add(currentTop); // note that we do NOT store (currentTop + 1) here; instead we store unmodified currentTop value that the match was found at
							bottomMatches.add(currentBottom);
							// combine both sets into a hashset to ensure there are no duplicates
							HashSet<Integer> matchSet = new HashSet<>();
							matchSet.addAll(topMatches);
							matchSet.addAll(bottomMatches);

							// now convert the set of all matches back into a list
							List<Integer> allMatches = new ArrayList<>();
							allMatches.addAll(matchSet);

							// create a new Posting that maps the current docId to a list of all matches from the top and bottom
							Posting matchPosting = new Posting(top.get(topPostingIndex).getDocumentId(), allMatches);
							System.out.println("Testing new posting of matches: " + matchPosting.toString());

							// add the new Posting to the mergedPostings results list
							mergedPostings.add(matchPosting);
						}
						// increment both indexes regardless of if the matching was a true success or not
						bottomTermIndex++;
						topTermIndex++;
					}

					// now we must handle the remaining cases, in which a successful matching is not immediately found but is still possible
					// this can happen in one of two ways:
					else {
						// the first (and easier) way is when the top's current term position is greater than the bottom's
						if (bottomMargin <= 0) {
							if (bottomTermIndex < bottomTermPositions.get(bottomTermPositions.size() - 1)) {
								bottomTermIndex++;

//							else {
										// this is handled by incrementing the bottom's index until the new currentBottom is no longer smaller than the currentTop
								// update currentBottom to recalculate the difference
								//currentBottom = bottomTermPositions.get(bottomTermIndex);
								//bottomMargin = currentBottom - currentTop;
							}
						}
						// the second (more difficult) way is when the bottom's current term location is still greater than the top's, buy by a difference of more than 1
						if (bottomMargin > 1) {
							if (topTermIndex < topTermPositions.get(topTermPositions.size() - 1)) {
//								break;
//								continue;
								topTermIndex++;

//							else {
								// this is handled by incrementing the top's index until the new bottom margin is at most 1
								// reassign currentTop with the incremented index
//								currentTop = topTermPositions.get(topTermIndex);
//								bottomMargin = currentBottom - currentTop;
							}
						}
					}
					// finally, increment both posting indexes before continuing to iterate
					topPostingIndex++;
					bottomPostingIndex++;
				}
			}
		}
		return mergedPostings;
	}

	
	@Override
	public String toString() {
		return "\"" + String.join(" ", mTerms) + "\"";
	}
}
