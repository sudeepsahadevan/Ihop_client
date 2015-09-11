package nw.itw.unibn;


import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/*
 * class to take two gene names as input and retrieve evidence sentences for each of the 
 * gene names from IHOP web client, compare the evidence sentences and output a common sentence
 * where the genes/synonyms co-occur. 
 */

public class IHOPclient {
	
	private Map<String, GeneEvidence> evidenceMap = new HashMap<String, GeneEvidence>();
	private DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
	
	public void findCoOcuurences(String gene1, String gene2,boolean abstractLevel){
//		abstractLevel boolean : print abstract level comention results even if sentence level results are available
		System.err.print("looking evidences for "+gene1+" ... ");
		getEvidences(gene1);
		System.err.print("looking evidences for "+gene2+" ... ");
		getEvidences(gene2);
		getInteractionEvidence(abstractLevel);
	}

	private void getEvidences(String gene){
//		"inspiration" for REST parser  from
		GeneEvidence genEvidence = new GeneEvidence();
		try {
			URL url = new URL("http://ws.bioinfo.cnio.es/iHOP/cgi-bin/getSymbolInteractions?gene=&synonym="+gene+"&ncbiTaxId=9606&reference=&namespace=&ihopid=");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(60000);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			System.err.println(conn.getResponseMessage());
			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "+ conn.getResponseCode());
			}
			DocumentBuilder xmlBuilder = xmlFactory.newDocumentBuilder();
			Document intXml = xmlBuilder.parse(conn.getInputStream());
			conn.disconnect();
//			every evidence in  returned XML is under tag iHOPsentence
			NodeList sentenceList = intXml.getElementsByTagName("iHOPsentence");
			for(int s=0;s<sentenceList.getLength();s++){
				boolean addSentence = true; // boolean indicating whether to add sentence to pmid2SentenceMap;
//				if there are more than 1 evidence symbol for a gene, it is considered ambiguous and hence not added
				StringBuilder sb = new StringBuilder();
				String pmid=sentenceList.item(s).getAttributes().getNamedItem("pmid").getNodeValue();
				Element sItem = (Element) sentenceList.item(s);
				NodeList sItemChilds=sItem.getChildNodes();
				for(int n=0;n<sItemChilds.getLength();n++){
					Node sItemNode= sItemChilds.item(n);
					if(sItemNode.getNodeType()==Node.ELEMENT_NODE){
						Element sItemElement = (Element) sItemNode;
						NodeList evidenceList = sItemElement.getElementsByTagName("evidence");
						if(evidenceList.getLength()>0){
							Set<String> symbolList = new HashSet<String>();
							for(int e=0;e<evidenceList.getLength();e++){
								NamedNodeMap evidenceMap = evidenceList.item(e).getAttributes();
								String symbol = evidenceMap.getNamedItem("symbol").getNodeValue();
								symbolList.add(symbol.toLowerCase());
							}
							if(symbolList.size()<2){
								List<String> symbols = new ArrayList<String>(symbolList);
								sb.append(symbols.get(0)+"@"+sItemElement.getTextContent());
							}else{
								addSentence=false; // ambigous sentence
								}
						}else{
							sb.append(sItemNode.getTextContent());
						}
					}else{
						sb.append(sItemNode.getTextContent());
					}
				}
				if((addSentence)&&(sb.length()>1)){
					genEvidence.addSentences(pmid, sb.toString());
				}
			}
			evidenceMap.put(gene, genEvidence);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void getInteractionEvidence(boolean abstractLevel){
		Set<String> outResult = new HashSet<String>();
//		System.out.println(evidenceMap.size());
		for(String gene1: evidenceMap.keySet()){
			GeneEvidence gene1Evidence = evidenceMap.get(gene1);
			Map<String, Set<String>> textEvidence1 = gene1Evidence.getEvidences();
//			System.out.println(textEvidence1.size());
			for(String gene2: evidenceMap.keySet()){
				if(!(gene1.equalsIgnoreCase(gene2))){
					GeneEvidence gene2Evidence = evidenceMap.get(gene2);
					Map<String, Set<String>> textEvidence2 = gene2Evidence.getEvidences();
					Set<String> commonPmids = new HashSet<String>(textEvidence2.keySet());
//					System.out.println(textEvidence1.keySet());
//					System.out.println(textEvidence2.keySet());
					commonPmids.retainAll(textEvidence1.keySet());
//					System.out.println(commonPmids);
					if(commonPmids.size()>0){
						for(String pmid: commonPmids){
							Set<String> evidencePmid1 = textEvidence1.get(pmid);
							Set<String> evidencePmid2 = textEvidence2.get(pmid);
							evidencePmid2.retainAll(evidencePmid1);
							if(evidencePmid2.size()>0){
								for(String evidence: evidencePmid2){
									String text=pmid+" :: "+evidence;
									text=text.replaceAll("\\n", "    ");
//									System.out.println(pmid+" :: "+evidence);
									outResult.add(text);
								}
							}
						}
					}
				}
			}
		}
		if(outResult.size()>0){
			System.out.println("_________ RESULTS _________ ");
			for(String s:outResult){
				System.out.println(s);
			}
			System.out.println("_________ END _________ ");
			if(abstractLevel){
				System.out.println();
				System.out.println("_________ Abstract level _________ ");
				getComentionEvidence();
			}
		}else{
			System.err.println("no sentence level co-ocurrence, looking for abstract level");
			getComentionEvidence();
		}
		
	}
	
	public void getComentionEvidence(){
//		lowering the standards, if there are no interaction evidences, look if the genes are mentioned in
//		same abstract
		Map<String, Set<String>> comentionEvidenceMap = new HashMap<String, Set<String>>();
		for(String gene1: evidenceMap.keySet()){
			GeneEvidence gene1Evidence = evidenceMap.get(gene1);
			Map<String, Set<String>> textEvidence1 = gene1Evidence.getEvidences();
			for(String gene2: evidenceMap.keySet()){
				if(!(gene1.equalsIgnoreCase(gene2))){
					GeneEvidence gene2Evidence = evidenceMap.get(gene2);
					Map<String, Set<String>> textEvidence2 = gene2Evidence.getEvidences();
					Set<String> commonPmids = new HashSet<String>(textEvidence2.keySet());
					commonPmids.retainAll(textEvidence1.keySet());
					if(commonPmids.size()>0){
						for(String pmid: commonPmids){
							Set<String> evidencePmid1 = textEvidence1.get(pmid);
							Set<String> evidencePmid2 = textEvidence2.get(pmid);
							evidencePmid1.addAll(evidencePmid2);
							comentionEvidenceMap.put(pmid, evidencePmid1);
						}
					}
				}
			}
		}
		
		if(comentionEvidenceMap.size()>0){
			System.out.println("_________ RESULTS _________ ");
			for(String pmid: comentionEvidenceMap.keySet()){
				System.out.println(pmid);
				Set<String> comentions = comentionEvidenceMap.get(pmid);
				for(String sent : comentions){
					System.out.println("\t"+sent);
				}
				System.out.println("--------------------");
			}
			System.out.println("_________ END _________ ");
		}else{
			System.err.println("no evidence at abstract level");
		}
	}
	
	public static void main(String[] args) {
		
		Options options = new Options();
		options.addOption("gene1",true,"Give the name for the first gene");
		options.addOption("gene2",true,"Give the name for the second gene");
		options.addOption("abstract",true,"Boolean, whether to look for abstract level evidence");
		options.addOption("h", "help", false, "Print this help info");
//		parse options
		CommandLineParser clip = new BasicParser();
		CommandLine cline = null;
		try {
			cline = clip.parse(options, args);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		IHOPclient ihop  = new IHOPclient();
		
		if(cline.hasOption("gene1") && cline.hasOption("gene2")){
			String g1 = cline.getOptionValue("gene1");
			String g2 = cline.getOptionValue("gene2");
			Boolean absLevel = false;
			if(cline.hasOption("abstract")){
				String boolstring = cline.getOptionValue("abstract");
				absLevel = Boolean.parseBoolean(boolstring);
				if(absLevel){
					System.err.println("Getting abstract level evidences");
				}
			}
			ihop.findCoOcuurences(g1, g2,absLevel);
		}else{
			HelpFormatter help = new HelpFormatter();
			String header ="Error! -gene1 and -gene2 parameters MUST BE GIVEN";
			String footer = "End of help";
			help.printHelp("java -jar jarname.jar", header, options, footer);
		}
		
	}
}
