package nw.itw.unibn;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*
 * class to store all the gene evidences from iHOP webservices
  
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.

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
