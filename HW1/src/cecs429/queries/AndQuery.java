package cecs429.queries;

import java.util.*;
import java.util.stream.Collectors;

import cecs429.indexes.Index;
import cecs429.indexes.Posting;
import cecs429.text.AdvancedTokenProcessor;
import cecs429.text.TokenProcessor;

import javax.imageio.ImageTranscoder;

/**
 * An AndQuery composes other QueryComponents and merges their postings in an intersection-like operation.
 * An AndQuery occurs when there are more than one consecutive QueryComponents that do not have the '+' between them
 */
public class AndQuery implements QueryComponent {
	private final List<QueryComponent> mComponents;
	private static List<String> mProcessedTerms;
//	private static HashSet<Integer> mDocIds;
//	private static List<Posting> mResults;

	public AndQuery(List<QueryComponent> components) {
		mComponents = components;
//		mDocIds = new HashSet<>();
//		mResults = new ArrayList<>();
		mProcessedTerms = new ArrayList<>();

	}

	
	@Override
	public List<Posting> getPostings(TokenProcessor processor, Index index) {
		// process and normalize tokens into terms to use to search the index
		for (QueryComponent c : mComponents) {
			List<String> currentTerms = processor.processToken(c.toString());
			mProcessedTerms.addAll(currentTerms);
		}
		//Collections.sort(mProcessedTerms);

		// initialize a master postings list with the postings for the first processed term in the list
		List<Posting> masterPostingsList = index.getPostings(mProcessedTerms.get(0));

		//mResults = mComponents.get(0).getPostings(index);
		try {
			for (int i = 1; i < mProcessedTerms.size(); i++) {
				masterPostingsList = intersect(masterPostingsList, index.getPostings(mProcessedTerms.get(i)));
//				List<Posting> previousPostings;
//				// for the first QueryComponent only, automatically add it to the master postings list and skip any AND processing
//				if (i == 1) {
//					previousPostings = mComponents.get(0).getPostings(index);
//				}
//				// for every other query component after that, update the master posting list by AND-ing it with the current component's postings list
//				else {
//					previousPostings = mResults;
//				}
//				List<Posting> currentPostings = mComponents.get(i).getPostings(index);
//				intersect(previousPostings, currentPostings);
			}
			return masterPostingsList;
		}
		catch (Exception ex) {
			System.out.println("No documents were found containing the AND query '" + this + "'");
			return null;
		}
	}

	// performs an AND intersect merge of two lists of postings to return a new list containing only the postings the are found in both of the original lists
	public List<Posting> intersect(List<Posting> top, List<Posting> bottom) {

		// initialize the results list
		List<Posting> results = new ArrayList<>();

		// set up indexes to iterate through both postings lists simultaneously
		int i = 0; // top index
		int j = 0; // bottom index

		// iterate through both lists simultaneously
		while (i < top.size() && j < bottom.size()) {
			// find the documentId's of each list at their respective current indexes
			int topDocId = top.get(i).getDocumentId();
			int bottomDocId = bottom.get(j).getDocumentId();

			// if they are equal, merge the two postings and add to the results
			if (topDocId == bottomDocId) {
//				// merge the postings of the current docId from both lists
//				Posting match = top.get(i).merge(bottom.get(j));
//				Collections.sort(match.getTermPositions());
//				// check if there is already a posting with this docId in the results
//				if (mDocIds.contains(topDocId)) {
//					// find the posting that already has this docId
//					Posting existing = mResults.stream().filter(posting -> topDocId == posting.getDocumentId()).findFirst().orElse(null);
//					// merge it with the match posting
//					Posting merge = match.merge(existing);
//					// must re-sort term positions after the merging
//					Collections.sort(merge.getTermPositions());
//					// now remove the existing posting and replace it with the merged posting
//					mResults.remove(existing);
//					mResults.add(merge);
//				} else {
//					mResults.add(match);
//				}
//				mResults.sort(Comparator.comparingInt(Posting::getDocumentId));
//				// add the docId to the set of added docId's
//				mDocIds.add(topDocId);
				// try to find an existing posting with the current docId
				Posting match = (top.get(i).merge(bottom.get(j)));
				Collections.sort(match.getTermPositions());
				results.add(match);
				i++;
				j++;
			}
			// if the current docId's aren't equal, increment the one with the lower docId
			else if (topDocId < bottomDocId) {
				i++;
			}
			else {
				j++;
			}
		}
		return results;
	}

	@Override
	public String toString() {
		// Returns a string of the form "[SUBQUERY] + [SUBQUERY] + [SUBQUERY]"
		return "Query components: (" +
				String.join(" + ", mComponents.stream().map(c -> c.toString()).collect(Collectors.toList()) + "\n Processed terms: (" + String.join(" + ", mProcessedTerms.stream().map(c -> c.toString()).collect(Collectors.toList())));
	}

}
