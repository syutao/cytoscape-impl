package org.cytoscape.cpath2.internal.util;

/*
 * #%L
 * Cytoscape CPath2 Impl (cpath2-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2010 - 2013
 *   University of Toronto
 *   Memorial Sloan-Kettering Cancer Center
 *   The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.biopax.paxtools.controller.AbstractTraverser;
import org.biopax.paxtools.controller.ObjectPropertyEditor;
import org.biopax.paxtools.controller.PropertyEditor;
import org.biopax.paxtools.controller.SimpleEditorMap;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.BioSource;
import org.biopax.paxtools.model.level3.CellularLocationVocabulary;
import org.biopax.paxtools.model.level3.Entity;
import org.biopax.paxtools.model.level3.Interaction;
import org.biopax.paxtools.model.level3.Named;
import org.biopax.paxtools.model.level3.PhysicalEntity;
import org.biopax.paxtools.model.level3.PublicationXref;
import org.biopax.paxtools.model.level3.RelationshipTypeVocabulary;
import org.biopax.paxtools.model.level3.RelationshipXref;
import org.biopax.paxtools.model.level3.SimplePhysicalEntity;
import org.biopax.paxtools.model.level3.Stoichiometry;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.model.level3.XReferrable;
import org.biopax.paxtools.model.level3.Xref;
import org.biopax.paxtools.util.ClassFilterSet;
import org.biopax.paxtools.util.Filter;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BioPax Utility Class - is a BioPAX Model Adapter 
 * that also defines additional constants. 
 *
 * @author Ethan Cerami, Rex, Arman and Igor Rodchenkov
 * 
 * @CyAPI.Final.Class
 */
public final class BioPaxUtil {
	private static final Map<String,String> cellLocationMap;
	private static final Map<String,String> chemModificationsMap;
	
	public static final Logger log = LoggerFactory.getLogger(BioPaxUtil.class);
    
	public static final String BIOPAX_NETWORK = "BIOPAX_NETWORK";
	public static final String BIOPAX_RDF_ID = "URI";
	public static final String BIOPAX_ENTITY_TYPE = "BIOPAX_TYPE";
	public static final String BIOPAX_DATA = "BIOPAX_DATA";
    public static final String DEFAULT_CHARSET = "UTF-8";
    public static final int MAX_DISPLAY_STRING_LEN = 25;
	public static final String NULL_ELEMENT_TYPE = "BioPAX Element";
	public static final String BIOPAX_EDGE_TYPE = "BIOPAX_EDGE_TYPE";
	public static final String BIOPAX_CHEMICAL_MODIFICATIONS_MAP = "CHEMICAL_MODIFICATIONS_MAP";
	public static final String BIOPAX_CHEMICAL_MODIFICATIONS_LIST = "CHEMICAL_MODIFICATIONS";
	public static final String BIOPAX_UNIFICATION_REFERENCES = "UNIFICATION_REFERENCES";
	public static final String BIOPAX_RELATIONSHIP_REFERENCES = "RELATIONSHIP_REFERENCES";
	public static final String BIOPAX_PUBLICATION_REFERENCES = "PUBLICATION_REFERENCES";
	public static final String BIOPAX_XREF_IDS = "IDENTIFIERS";
	public static final String BIOPAX_XREF_PREFIX = "ID_";
	public static final String BIOPAX_IHOP_LINKS = "IHOP_LINKS";
	public static final String BIOPAX_AFFYMETRIX_REFERENCES_LIST = "AFFYMETRIX_REFERENCES";
	public static final String PHOSPHORYLATION_SITE = "phosphorylation site";
	public static final String PROTEIN_PHOSPHORYLATED = "Protein-phosphorylated";
	
	// private Constructor
	private BioPaxUtil() {}
	
	static  {
		// the following is for node labels
		cellLocationMap = new HashMap<String, String>();
		cellLocationMap.put("cellular component unknown", "");
		cellLocationMap.put("centrosome", "CE");
		cellLocationMap.put("cytoplasm", "CY");
		cellLocationMap.put("endoplasmic reticulum", "ER");
		cellLocationMap.put("endosome", "EN");
		cellLocationMap.put("extracellular", "EM");
		cellLocationMap.put("golgi apparatus", "GA");
		cellLocationMap.put("mitochondrion", "MI");
		cellLocationMap.put("nucleoplasm", "NP");
		cellLocationMap.put("nucleus", "NU");
		cellLocationMap.put("plasma membrane", "PM");
		cellLocationMap.put("ribosome", "RI");
		cellLocationMap.put("transmembrane", "TM");
		
		chemModificationsMap = new HashMap<String, String>();
		chemModificationsMap.put("acetylation site", "A");
		chemModificationsMap.put("glycosylation site", "G");
		chemModificationsMap.put("phosphorylation site", "P");
		chemModificationsMap.put("proteolytic cleavage site", "PCS");
		chemModificationsMap.put("sumoylation site", "S");
		chemModificationsMap.put("ubiquitination site", "U");	
	}
	

	/**
	 * Gets or infers the name of the node. 
	 * 
	 * @param bpe BioPAX Element
	 * @return
	 */
	private static String getNodeName(BioPAXElement bpe) {

		if(bpe == null) {
			return "";
		}
				
		String nodeName = getShortName(bpe);
		if (nodeName == null || nodeName.length()== 0) {
			nodeName = getStandardName(bpe);
			if (nodeName == null || nodeName.length() == 0) {
				Collection<String> names = getSynonyms(bpe);
				if (!names.isEmpty())
					nodeName = getTheShortestString(names);
			}
		}

		return (nodeName == null || nodeName.length() == 0)
				? bpe.getRDFId() : StringEscapeUtils.unescapeHtml(nodeName);
	}
	
	
	// get the shortest string
	private static String getTheShortestString(Collection<String> nameList) {
		String shortest = null;
		if (nameList != null && !nameList.isEmpty()) {
			int minLength = -1;
			for (String name: nameList) {
				if ( name.length() < minLength || minLength == -1) {
					minLength = name.length();
					shortest = name;
				}
			}
		}
		return shortest;
	}
	
	
	/**
	 * Attempts to get the value of any of the BioPAX properties
	 * in the list.
	 * @param bpe BioPAX Element
	 * @param properties BioPAX property names
	 * 
	 * @return the value or null
	 */
	private static Object getValue(BioPAXElement bpe, String... properties) {
		for (String property : properties) {
			try {
				Method method = bpe.getModelInterface().getMethod(
						"get" + property.substring(0, 1).toUpperCase()
								+ property.substring(1).replace('-', '_'));
				Object invoke = method.invoke(bpe);
				if (invoke != null) {
					return invoke;
				}
//				PropertyEditor editor = SimpleEditorMap.L3
//					.getEditorForProperty(property, bpe.getModelInterface());
//				return editor.getValueFromBean(bpe); // is always a Set!
			} catch (Exception e) {
				if(log.isDebugEnabled()) {
					// this is often OK, as we guess L2 or L3 properties...
					log.debug("Ignore property " + property + " for " 
						+ bpe.getRDFId() + ": " + e);
				}
			}
		}
		return null;
	}
	
	
	/**
	 * Attempts to get the values of specified BioPAX properties.
	 * @param bpe BioPAX Element
	 * @param properties BioPAX property names
	 * 
	 * @return the set of property values or null
	 */
	private static Collection<?> getValues(BioPAXElement bpe, String... properties) {
		Collection<Object> col = new HashSet<Object>();
		
		for (String property : properties) {
			try {
				Method method = bpe.getModelInterface().getMethod(
						"get" + property.substring(0, 1).toUpperCase()
								+ property.substring(1).replace('-', '_'));
				
				Object invoke = method.invoke(bpe);
				if (invoke != null) {
					// return value can be collection or Object
					if (invoke instanceof Collection) {
						col.addAll((Collection) invoke);
					} else {
						col.add(invoke);
					}
				}
			} catch (Exception e) {
				if(log.isDebugEnabled()) {
					log.debug("Cannot get value of '" + property + "' for "
						+ bpe.getRDFId() + ": " + e);
				}
			}
		}
		
		return col;
	}
	
	
	/**
	 * Gets the Short Name (or Display Name).
	 *
	 * @param bpe BioPAX element
	 * @return short name field, or null if not available.
	 */
	private static String getShortName(BioPAXElement bpe) {
		String shortName = null;
		
		if(bpe instanceof Named) {
			shortName = ((Named)bpe).getDisplayName();
		} 
	
		return shortName;
	}

	/**
	 * Gets the Name Field.
	 *
	 * @param bpe BioPAX element
	 * @return name field, or null if not available.
	 */
	private static String getStandardName(BioPAXElement bpe) {
		if(bpe instanceof Named) {
			return ((Named)bpe).getStandardName();
		} else 
			return null;
	}

	/**
	 * Gets all names, if any present.
	 *
	 * @param bpe BioPAX element
	 * @return Collection of names.
	 */
	private static Collection<String> getSynonyms(BioPAXElement bpe) {
		Collection<String> names = new HashSet<String>();
		if(bpe instanceof Named) {
			names = ((Named)bpe).getName();
		}
		return names;
	}

	
	/**
	 * Gets the NCBI Taxonomy ID.
	 *
	 * @param bpe BioPAX element
	 * @return taxonomyId, or -1, if not available.
	 */
	private static int getOrganismTaxonomyId(CyNetwork network, BioPAXElement bpe) {
		int taxonomyId = -1;
		
		try {
			Object bs = getValue(bpe, "organism");
			if (bs instanceof BioSource) {
				Set<Xref> xrefs = ((BioSource)bs).getXref();
				if(!xrefs.isEmpty()) {
					Xref tx = xrefs.iterator().next();
					taxonomyId = Integer.parseInt(tx.getId());
				}
			}
		} catch (Exception e) {
			taxonomyId = -1;
		}

		return taxonomyId;
	}

	
	
	private static <T extends Xref> List<T> getXRefs(BioPAXElement bpe, Class<T> xrefClass) {
		if(bpe instanceof XReferrable) {
			List<T> erefs = new ArrayList<T>();
			erefs.addAll(new ClassFilterSet<Xref,T>( ((XReferrable)bpe).getXref(), xrefClass) );
			if(bpe instanceof SimplePhysicalEntity && 
				((SimplePhysicalEntity)bpe).getEntityReference() != null)
			{
				erefs.addAll(new ClassFilterSet<Xref,T>(
					((SimplePhysicalEntity)bpe).getEntityReference().getXref(), xrefClass) );
			}
			return erefs;
		}
		return new ArrayList<T>();
	}
	
	
	/**
	 * Gets abbreviated cellular location term.
	 * 
	 * @param value
	 * @return
	 */
	private static String getAbbrCellLocation(String value) {
		for(String abr: cellLocationMap.keySet()) {
			if(value.toLowerCase().contains(abr)) {
				return cellLocationMap.get(abr);
			}
		}
		return value;
	}
	
	/**
	 * Gets abbreviated chemical modification term.
	 * 
	 * @param value
	 * @return
	 */
	private static String getAbbrChemModification(String value) {
		for(String abr: chemModificationsMap.keySet()) {
			if(value.toLowerCase().contains(abr)) {
				return chemModificationsMap.get(abr);
			}
		}
		return value;
	}
	
	
	private static String truncateLongStr(String str) {
		if(str != null) {
			str = str.replaceAll("[\n\r \t]+", " ");
			if (str.length() > MAX_DISPLAY_STRING_LEN) {
				str = str.substring(0, MAX_DISPLAY_STRING_LEN/2-1) 
					+ "..." + str.substring(str.length() - MAX_DISPLAY_STRING_LEN/2);
			}
		}
		return str;
	}

	
	private static String toOwl(BioPAXElement bpe) {
		StringWriter writer = new StringWriter();
		try {
			SimpleIOHandler simpleExporter = new SimpleIOHandler(BioPAXLevel.L3);
			simpleExporter.writeObject(writer, bpe);
		} catch (Exception e) {
			log.error("Failed printing '" + bpe.getRDFId() + "' to OWL", e);
		}
		return writer.toString();
	}
	
	
	/**
	 * Generates node attributes from BioPAX properties.
	 * 
	 * @param element
	 * @param node
	 * @param network
	 */
	public static void createAttributesFromProperties(final BioPAXElement element,
			final CyNode node, final CyNetwork network) 
	{
		Filter<PropertyEditor> filter = new Filter<PropertyEditor>() {
			@Override
			// skips for entity-range properties (which map to edges rather than attributes!),
			// and several utility classes ranges (for which we do not want generate attributes or do another way)
			public boolean filter(PropertyEditor editor) {
				boolean pass = true;
				
				final String prop = editor.getProperty();
				if(editor instanceof ObjectPropertyEditor) {
					Class c = editor.getRange();
					if( Entity.class.isAssignableFrom(c)
						|| Stoichiometry.class.isAssignableFrom(c)
						|| "nextStep".equals(prop) 
						) {	
						pass = false;; 
					}
				} else if("name".equals(prop))
					pass = false;
				
				return pass;
			}
		};
		
		@SuppressWarnings("unchecked")
		AbstractTraverser bpeAutoMapper = new AbstractTraverser(SimpleEditorMap.L3, filter) 
		{
			@SuppressWarnings("rawtypes")
			@Override
			protected void visit(Object obj, BioPAXElement bpe, Model model,
					PropertyEditor editor) 
			{
				String attrName = getAttrName(getProps());
				if (obj != null && !editor.isUnknown(obj)) {
					String value = obj.toString();
					if (!"".equalsIgnoreCase(value.toString().replaceAll("\\]|\\[", ""))) 
					{
						if (editor.isMultipleCardinality()) {
							CyRow row = network.getRow(node);
							List vals = new ArrayList<String>();
							// consider existing attribute values
							if (row.isSet(attrName)) {
								Class<?> listElementType = row.getTable()
										.getColumn(attrName).getListElementType();
								List prevList = row.getList(attrName, listElementType);
								if (prevList != null)
									vals = prevList;
							} 
						
							if(!vals.contains(value)) 
								vals.add(value);

							AttributeUtil.set(network, node, attrName, vals, String.class);
						} else {
							AttributeUtil.set(network, node, attrName, value, String.class);
						}
					}
					
					// currently, we don't map absolutely all BioPAX relationships to edges/attributes: 
					// traverse deeper only if it's an object range property
					// or single cardinality (- otherwise would 
					// result with having too many/branchy Cy attributes)
					if (editor instanceof ObjectPropertyEditor
							&& !editor.isMultipleCardinality()) 
					// this effectively prevents going into details for
					// such objects as values of 'xref', 'memberEntityReference', 
					// 'componentStoichiometry', etc. props.
					{
						traverse((BioPAXElement) obj, null);
					}
				}
			}

			private String getAttrName(Stack<String> props) {
				return StringUtils.join(props, "/");
			}
		};

		// set the most important attributes
		AttributeUtil.set(network, node, BIOPAX_RDF_ID, element.getRDFId(), String.class);
		AttributeUtil.set(network, node, BIOPAX_ENTITY_TYPE, element.getModelInterface().getSimpleName(), String.class);	
		
		// add a piece of the BioPAX (RDF/XML without parent|child elements)
		
		String owl = toOwl(element); // (requires common-lang-2.4 bundle to be started)
		AttributeUtil.set(network, node, CyNetwork.HIDDEN_ATTRS, BIOPAX_DATA, owl, String.class);
		
		String name = BioPaxUtil.truncateLongStr(BioPaxUtil.getNodeName(element) + "");
		
		if (!(element instanceof Interaction)) {
			// get chemical modification & cellular location attributes
			NodeAttributesWrapper chemicalModificationsWrapper = getInteractionChemicalModifications(element);
			// add modifications to the label/name
			String modificationsString = getModificationsString(chemicalModificationsWrapper);
			name += modificationsString;				
			// add cellular location to the label/name
			if(element instanceof PhysicalEntity) {
				CellularLocationVocabulary cl = ((PhysicalEntity) element).getCellularLocation();
				if(cl != null) {
					String clAbbr = BioPaxUtil.getAbbrCellLocation(cl.toString())
						.replaceAll("\\[|\\]", "");
					name += (clAbbr.length() > 0) ? ("\n" + clAbbr) : "";
				}
			}
			// set node attributes
			setChemicalModificationAttributes(network, node, chemicalModificationsWrapper);	
		}
		// update the name (also used for node's label and quick find)
		AttributeUtil.set(network, node, CyNetwork.NAME, name, String.class);		
		
		// traverse to create the rest of attr.
		bpeAutoMapper.traverse(element, null);
		
        // create custom (convenience?) attributes, mainly - from xrefs
		createExtraXrefAttributes(element, network, node);
	}

	
	/**
	 * A helper function to set chemical modification attributes
	 */
	private static void setChemicalModificationAttributes(CyNetwork network, CyNode node, 
			NodeAttributesWrapper chemicalModificationsWrapper) 
	{
		Map<String, Object> modificationsMap = (chemicalModificationsWrapper != null)
				? chemicalModificationsWrapper.getMap() : null;

		if (modificationsMap != null) {

			//  As discussed with Ben on August 29, 2006:
			//  We will now store chemical modifications in two places:
			//  1.  a regular list of strings (to be used by the view details panel,
			//  node attribute browser, and Quick Find.
			//  2.  a multihashmap, of the following form:
			//  chemical_modification --> modification(s) --> # of modifications.
			//  this cannot be represented as a SimpleMap, and must be represented as
			//  a multi-hashmap.  This second form is used primarily by the custom
			//  rendering engine for, e.g. drawing number of phosphorylation sies.

			//  Store List of Chemical Modifications Only
			List<String> list = new ArrayList<String>(modificationsMap.keySet());
			AttributeUtil.set(network, node, BIOPAX_CHEMICAL_MODIFICATIONS_LIST, list, String.class);

			//  Store Complete Map of Chemical Modifications --> # of Modifications
			// TODO: How do we handle MultiHashMaps?
//			setMultiHashMap(cyNodeId, nodeAttributes, BIOPAX_CHEMICAL_MODIFICATIONS_MAP, modificationsMap);

			if (modificationsMap.containsKey(BioPaxUtil.PHOSPHORYLATION_SITE)) {
				AttributeUtil.set(network, node, BIOPAX_ENTITY_TYPE, BioPaxUtil.PROTEIN_PHOSPHORYLATED, String.class);
			}
		}
	}

	
    private static void createExtraXrefAttributes(BioPAXElement resource, CyNetwork network, CyNode node) {
		// the following code should replace the old way to set
		// relationship references
		List<String> xrefList = getXRefList(resource,
				BIOPAX_AFFYMETRIX_REFERENCES_LIST);
		if ((xrefList != null) && !xrefList.isEmpty()) {
			AttributeUtil.set(network, node, BIOPAX_AFFYMETRIX_REFERENCES_LIST,
					xrefList, String.class);
		}
		
		// ihop links
		String stringRef = addIHOPLinks(network, resource);
		if (stringRef != null) {
			AttributeUtil.set(network, node, CyNetwork.HIDDEN_ATTRS, BIOPAX_IHOP_LINKS, stringRef, String.class);
		}

		List<String> allxList = new ArrayList<String>();
		List<String> unifxfList = new ArrayList<String>();
		List<String> relxList = new ArrayList<String>();
		List<String> pubxList = new ArrayList<String>();
		// add xref ids per database and per xref class
		List<Xref> xList = BioPaxUtil.getXRefs(resource, Xref.class);
		for (Xref link : xList) {
			// per db -
			String key = BIOPAX_XREF_PREFIX + link.getDb().toUpperCase();
			// Set individual XRefs; Max of 1 per database.
			String existingId = network.getRow(node).get(key, String.class);
			if (existingId == null) {
				AttributeUtil.set(network, node, key, link.getId(), String.class);
			}
			

			StringBuffer temp = new StringBuffer();
			
			if(!"CPATH".equalsIgnoreCase(link.getDb()))
				temp.append(ExternalLinkUtil.createLink(link.getDb(), link.getId()));
			else
				temp.append(link.toString());
			
			if(link instanceof UnificationXref) {
				unifxfList.add(temp.toString());
			}
			else if(link instanceof PublicationXref) {
				PublicationXref xl = (PublicationXref) link;
				temp.append(" ");
				if (!xl.getAuthor().isEmpty()) {
					temp.append(xl.getAuthor().toString() + " et al., ");
				}
				if (xl.getTitle() != null) {
					temp.append(xl.getTitle());
				}
				if (!xl.getSource().isEmpty()) {
					temp.append(" (" + xl.getSource().toString());
					if (xl.getYear() > 0) {
						temp.append(", " + xl.getYear());
					}
					temp.append(")");
				}
				pubxList.add(temp.toString());
			}
			else if(link instanceof RelationshipXref) {
				relxList.add(temp.toString());
			}
			
			allxList.add(link.toString());
		}
		
		AttributeUtil.set(network, node, BIOPAX_XREF_IDS, allxList, String.class);
		AttributeUtil.set(network, node, CyNetwork.HIDDEN_ATTRS, BIOPAX_UNIFICATION_REFERENCES, unifxfList, String.class);
		AttributeUtil.set(network, node, CyNetwork.HIDDEN_ATTRS, BIOPAX_RELATIONSHIP_REFERENCES, relxList, String.class);
		AttributeUtil.set(network, node, CyNetwork.HIDDEN_ATTRS, BIOPAX_PUBLICATION_REFERENCES, pubxList, String.class);	
	}

    
	private static String addIHOPLinks(CyNetwork network, BioPAXElement bpe) {
		List<String> synList = new ArrayList<String>(BioPaxUtil.getSynonyms(bpe));
		List<ExternalLink> dbList = xrefToExternalLinks(bpe, Xref.class);
		
		if (!synList.isEmpty() || !dbList.isEmpty()) {
			String htmlLink = ExternalLinkUtil.createIHOPLink(bpe.getModelInterface().getSimpleName(),
					synList, dbList, BioPaxUtil.getOrganismTaxonomyId(network, bpe));
			if (htmlLink != null) {
				return htmlLink;
			}
		}

		return null;
	}

	
	private static <T extends Xref> List<ExternalLink> xrefToExternalLinks(BioPAXElement bpe, Class<T> xrefClass) {
		
		if(bpe instanceof XReferrable) {
			List<ExternalLink> erefs = new ArrayList<ExternalLink>();
			erefs.addAll(extractXrefs(new ClassFilterSet<Xref,T>(
				((XReferrable)bpe).getXref(), xrefClass) ));
			if(bpe instanceof SimplePhysicalEntity && 
				((SimplePhysicalEntity)bpe).getEntityReference() != null)
			{
				erefs.addAll(extractXrefs(new ClassFilterSet<Xref,T>(
					((SimplePhysicalEntity)bpe).getEntityReference().getXref(), xrefClass) ));
			}
			return erefs;
		}
		return new ArrayList<ExternalLink>();
	}
	
	private static List<ExternalLink> extractXrefs(Collection<? extends Xref> xrefs) {
		List<ExternalLink> dbList = new ArrayList<ExternalLink>();

		for (Xref x: xrefs) {		
			String db = null;
			String id = null;
			String relType = null;
			String title = null;
			String year = null;
			String author = null;
			String url = null;
			String source = null;
			
			db = x.getDb();
			String ver = x.getIdVersion();
			id = x.getId(); // + ((ver!=null) ? "_" + ver : "");
			if(x instanceof RelationshipXref) {
				RelationshipTypeVocabulary v = ((RelationshipXref)x).getRelationshipType();
				if(v != null) relType = v.getTerm().toString();
			}
			if(x instanceof PublicationXref) {
				PublicationXref px = (PublicationXref)x;
				author = px.getAuthor().toString();
				title = px.getTitle();
				source = px.getSource().toString();
				url =px.getUrl().toString();
				year = px.getYear() + "";
			}

			if ((db != null) && (id != null)) {
				ExternalLink link = new ExternalLink(db, id);
				link.setAuthor(author);
				link.setRelType(relType);
				link.setTitle(title);
				link.setYear(year);
				link.setSource(source);
				link.setUrl(url);
				dbList.add(link);
			}
		}

		return dbList;
	}

	
	private static List<String> getXRefList(BioPAXElement bpe, String xrefType) {
		List<String> listToReturn = new ArrayList<String>();

		// get the xref list
		List<ExternalLink> list = xrefToExternalLinks(bpe, RelationshipXref.class);
		// what type of xref are we interested in ?
		String type = null;
		if (xrefType.equals(BIOPAX_AFFYMETRIX_REFERENCES_LIST)) {
			type = "AFFYMETRIX";
		}

		if (!list.isEmpty()) {
			for (ExternalLink link : list) {
				if (link.getDbName().toUpperCase().startsWith(type)) {
					listToReturn.add(link.getId());
				}
			}
		}

		return listToReturn;
	}

	
	private static NodeAttributesWrapper getInteractionChemicalModifications(BioPAXElement participantElement) 
	{
		
		if(participantElement == null) {
			return null;
		}
		
		// both of these objects will be used to contruct
		// the NodeAttributesWrapper which gets returned
		Map<String,Object> chemicalModificationsMap = null;
		String chemicalModifications = null;

		// if we are dealing with participant processes (interactions
		// or complexes), we have to go through the participants to get the
		// proper chemical modifications
		Collection<?> modificationFeatures =
				BioPaxUtil.getValues(participantElement, "feature", "notFeature");
		// short ciruit routine if empty list
		if (modificationFeatures == null) {
			return null;
		}

		// interate through the list returned from the query
		for (Object modification : modificationFeatures) {
			if (modification != null) {
				// initialize chemicalModifications string if necessary
				chemicalModifications = (chemicalModifications == null || chemicalModifications.length()==0) 
					? "-" : chemicalModifications;
				// initialize chemicalModifications hashmap if necessary
				chemicalModificationsMap = (chemicalModificationsMap == null) 
					? new HashMap<String, Object>() : chemicalModificationsMap;

				Object value = BioPaxUtil.getValue((BioPAXElement)modification, "modificationType");
				String mod = (value == null) ? "" : value.toString();
				
				// is this a new type of modification ?
				if (!chemicalModificationsMap.containsKey(mod)) {
					// determine abbreviation
					String abbr = BioPaxUtil.getAbbrChemModification(mod);

					// add abreviation to modifications string
					// (the string "-P...")
					chemicalModifications += abbr;

					// update our map - modification, count
					chemicalModificationsMap.put(mod, new Integer(1));
				} else {
					// we've seen this modification before, just update the count
					Integer count = (Integer) chemicalModificationsMap.get(mod);
					chemicalModificationsMap.put(mod, ++count);
				}
			}
		}

		return new NodeAttributesWrapper(chemicalModificationsMap, chemicalModifications);
	}

	
	private static String getModificationsString(NodeAttributesWrapper chemicalModificationsWrapper) 
	{

		// check args
		if (chemicalModificationsWrapper == null) return "";

		// get chemical modifications
		String chemicalModification = (chemicalModificationsWrapper != null)
			? chemicalModificationsWrapper.getAbbreviationString()
			: null;

		// outta here
		return (((chemicalModification != null) && (chemicalModification.length() > 0))
				? chemicalModification : "");
	}
}
