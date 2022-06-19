package cecs429.queries;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import cecs429.indexes.Index;
import cecs429.indexes.Posting;
import cecs429.text.AdvancedTokenProcessor;

/**
 * An AndQuery composes other QueryComponents and merges their postings in an intersection-like operation.
 * An AndQuery occurs when there are more than one consecutive QueryComponents that do not have the '+' between them
 */
public class AndQuery implements QueryComponent {
	private List<QueryComponent> mComponents;
	
	public AndQuery(List<QueryComponent> components) {
		mComponents = components;
	}
	
	@Override
	public List<Posting> getPostings(Index index) {
		// initialize a master postings list to which we will add all of the individual lists from each component after any necessary processing
		List<Posting> masterPostingsList = new ArrayList<>();
		try {
			for (int i = 0; i < mComponents.size(); i++) {
				// for the first QueryComponent only, automatically add it to the master postings list and skip any AND processing
				if (i == 0) {
					masterPostingsList.addAll(mComponents.get(i).getPostings(index));
				}
				// for every other query component after that, update the master posting list by AND-ing it with the current component's postings list
				else {
					masterPostingsList = intersect(mComponents.get(i).getPostings(index), masterPostingsList);
				}
			}
//
//			List<Posting> results = new ArrayList<>();
//
//			for (int i = 0; i < masterPostingsList.size() - 1; i++) {
//
//				Posting result = masterPostingsList.get(i);
//				Posting current = masterPostingsList.get(i);
//				Posting next = masterPostingsList.get(i + 1);
//
//				while (current.getDocumentId() == next.getDocumentId() && (i <= masterPostingsList.size() - 1)) {
//					current = masterPostingsList.get(i);
//					next = masterPostingsList.get(i + 1);
//
//					result = current.merge(next);
//					i++;
//				}
//
//				masterPostingsList.add(result);
//			}
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

			// if they are equal, that means both of the postings contain this docId
			if (topDocId == bottomDocId) {
				// so we add the intersected postings lists to the results list
				results.add(top.get(i));
				// increment both indexes simultaneously to continue iterating
				i++;
				j++;
			}

			// otherwise, increment the index currently pointing to the lower docId and continue iterating until there's another match
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
		return
		 String.join(" ", mComponents.stream().map(c -> c.toString()).collect(Collectors.toList()));
	}
}
