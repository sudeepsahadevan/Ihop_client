package nw.itw.unibn;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*
 * class to store all the gene evidences from iHOP webservices
 */
public class GeneEvidence {

	private Map<String, Set<String>> pmid2Sentence = new HashMap<String, Set<String>>();
	
	public void addSentences(String pmid,String sentence){
		Set<String> evidenceString = pmid2Sentence.get(pmid);
		if(evidenceString==null){
			evidenceString = new HashSet<String>();
			pmid2Sentence.put(pmid, evidenceString);
		}
		evidenceString.add(sentence);
	}
	
	public Set<String> getPmids(){
		return pmid2Sentence.keySet();
	}
	
	public Map<String, Set<String>> getEvidences(){
		return pmid2Sentence;
	}
}
