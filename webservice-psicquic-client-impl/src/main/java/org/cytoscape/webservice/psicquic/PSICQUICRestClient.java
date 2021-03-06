package org.cytoscape.webservice.psicquic;

/*
 * #%L
 * Cytoscape PSIQUIC Web Service Impl (webservice-psicquic-client-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2013 The Cytoscape Consortium
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

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.webservice.psicquic.mapper.MergedNetworkBuilder;
import org.cytoscape.webservice.psicquic.simpleclient.PSICQUICSimpleClient;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import psidev.psi.mi.tab.PsimiTabReader;
import psidev.psi.mi.tab.model.BinaryInteraction;
import uk.ac.ebi.enfin.mi.cluster.InteractionCluster;

/**
 * Light-weight REST client based on SimpleClient by EBI team.
 * 
 */
public final class PSICQUICRestClient {

	private static final Logger logger = LoggerFactory.getLogger(PSICQUICRestClient.class);

	// Static list of ID. The oreder is important!
	private static final String MAPPING_NAMES = "uniprotkb,chebi,ddbj/embl/genbank,ensembl,irefindex";

	public enum SearchMode {
		MIQL("Search by Query (MIQL)"), INTERACTOR("Search by gene/protein ID list"), SPECIES("Search by species");

		private final String name;

		private SearchMode(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return this.name;
		}
	}

	public static final Long ERROR_SEARCH_FAILED = -1l;
	public static final Long ERROR_TIMEOUT = -2l;
	public static final Long ERROR_CANCEL = -3l;

	// Timeout for search. TODO: Make public as property.
	private static final long SEARCH_TIMEOUT_MSEC = 10000;

	// Timeout for import. TODO: Make public as property.
	private static final long IMPORT_TIMEOUT = 1000;

	private final CyNetworkFactory factory;
	private final RegistryManager regManager;
	private final MergedNetworkBuilder builder;

	static boolean canceled = false;

	public PSICQUICRestClient(final CyNetworkFactory factory, final RegistryManager regManager,
			final MergedNetworkBuilder builder) {
		this.factory = factory;
		this.regManager = regManager;
		this.builder = builder;
	}

	public CyNetwork importMergedNetwork(final String query, final Collection<String> targetServices,
			final SearchMode mode, final TaskMonitor tm) throws IOException {

		final InteractionCluster importedCluster = importMerged(query, targetServices, mode, tm);
		final CyNetwork network = builder.buildNetwork(importedCluster);

		tm.setProgress(1.0d);
		return network;
	}

	public Collection<CyNetwork> importNetworks(final String query, final Collection<String> targetServices,
			final SearchMode mode, final TaskMonitor tm) throws IOException {

		final Set<CyNetwork> networks = new HashSet<CyNetwork>();
		final Map<String, Collection<BinaryInteraction>> result = importNetwork(query, targetServices, mode, tm);

		for (String source : result.keySet()) {
			tm.setStatusMessage("Merging results...");
			final InteractionCluster iC = new InteractionCluster();
			iC.setBinaryInteractionIterator(result.get(source).iterator());
			iC.setMappingIdDbNames(MAPPING_NAMES);
			iC.runService();

			final CyNetwork network = builder.buildNetwork(iC);
			final String networkName = regManager.getSource2NameMap().get(source);
			network.getRow(network).set(CyNetwork.NAME, networkName);
			networks.add(network);
		}

		tm.setProgress(1.0d);
		return networks;
	}

	public InteractionCluster importNeighbours(final String query, final Collection<String> targetServices,
			final SearchMode mode, final TaskMonitor tm) throws IOException {
		return importMerged(query, targetServices, mode, tm);
	}

	private final InteractionCluster importMerged(final String query, final Collection<String> targetServices,
			final SearchMode mode, final TaskMonitor tm) {

		final Map<String, Collection<BinaryInteraction>> result = importNetwork(query, targetServices, mode, tm);
		final Collection<Collection<BinaryInteraction>> binaryInteractions = result.values();
		final List<BinaryInteraction> allInteractions = new ArrayList<BinaryInteraction>();
		for (Collection<BinaryInteraction> interactions : binaryInteractions)
			allInteractions.addAll(interactions);

		tm.setStatusMessage("Merging results...");
		InteractionCluster iC = new InteractionCluster();
		iC.setBinaryInteractionIterator(allInteractions.iterator());
		iC.setMappingIdDbNames(MAPPING_NAMES);
		iC.runService();

		return iC;
	}

	private Map<String, Collection<BinaryInteraction>> importNetwork(final String query,
			final Collection<String> targetServices, final SearchMode mode, final TaskMonitor tm) {
		final Map<String, Collection<BinaryInteraction>> result = new HashMap<String, Collection<BinaryInteraction>>();

		canceled = false;

		tm.setTitle("Loading network data from Remote PSICQUIC Services");

		Map<String, CyNetwork> resultMap = new ConcurrentHashMap<String, CyNetwork>();
		final ExecutorService exe = Executors.newCachedThreadPool();
		final CompletionService<Collection<BinaryInteraction>> completionService = new ExecutorCompletionService<Collection<BinaryInteraction>>(
				exe);

		final long startTime = System.currentTimeMillis();

		// Submit the query for each active service
		double completed = 0.0d;
		final double increment = 1.0d / (double) targetServices.size();

		final SortedSet<String> sourceSet = new TreeSet<String>();
		final SortedSet<String> nameSet = new TreeSet<String>();
		final Set<ImportNetworkAsMitabTask> taskSet = new HashSet<ImportNetworkAsMitabTask>();
		for (final String serviceURL : targetServices) {
			nameSet.add(regManager.getSource2NameMap().get(serviceURL));
			final ImportNetworkAsMitabTask task = new ImportNetworkAsMitabTask(serviceURL, query, mode);
			completionService.submit(task);
			taskSet.add(task);
			sourceSet.add(serviceURL);
		}

		
		
		
		int i = 0;
		for (final String service : targetServices) {
			if (canceled) {
				logger.warn("Interrupted by user: network import task");
				exe.shutdownNow();
				resultMap.clear();
				resultMap = null;

				return null;
			}

			Future<Collection<BinaryInteraction>> future = null;
			try {
				future = completionService.take();
				final Collection<BinaryInteraction> ret = future.get();
				if (ret != null) {
					result.put(service, ret);
					// binaryInteractions.addAll(ret);
				}

				completed = completed + increment;
				tm.setProgress(completed);
				nameSet.remove(regManager.getSource2NameMap().get(service));
//				final StringBuilder sBuilder = new StringBuilder();
//				for (String sourceStr : sourceSet) {
//					sBuilder.append(regManager.getSource2NameMap().get(sourceStr) + " ");
//				}

				tm.setStatusMessage((i + 1) + " / " + targetServices.size() + " tasks finished.\n"
						+ "Still waiting responses from the following databases:\n\n" + nameSet.toString());

			} catch (InterruptedException ie) {
				for (final ImportNetworkAsMitabTask t : taskSet)
					t.cancel();

				taskSet.clear();

				List<Runnable> tasks = exe.shutdownNow();
				logger.warn("Interrupted: network import.  Remaining = " + tasks.size(), ie);
				resultMap.clear();
				resultMap = null;
				return null;
			} catch (ExecutionException e) {
				logger.warn("Error occured in network import", e);
				continue;
			}

			i++;
		}

		try {
			exe.shutdown();
			exe.awaitTermination(IMPORT_TIMEOUT, TimeUnit.SECONDS);

			long endTime = System.currentTimeMillis();
			double sec = (endTime - startTime) / (1000.0);
			logger.info("PSICUQIC Import Finished in " + sec + " sec.");
		} catch (Exception ex) {
			logger.warn("Import operation timeout", ex);
			return null;
		} finally {
			taskSet.clear();
			sourceSet.clear();
		}

		return result;
	}

	public Map<String, Long> search(final String query, final Collection<String> targetServices, final SearchMode mode,
			final TaskMonitor tm) {

		canceled = false;
		Map<String, Long> resultMap = new ConcurrentHashMap<String, Long>();

		final ExecutorService exe = Executors.newCachedThreadPool();
		final long startTime = System.currentTimeMillis();

		// Submit the query for each active service
		final List<SearchTask> tasks = new ArrayList<SearchTask>();
		for (final String serviceURL : targetServices)
			tasks.add(new SearchTask(serviceURL, query, mode));

		List<Future<Long>> futures;
		try {
			futures = exe.invokeAll(tasks, SEARCH_TIMEOUT_MSEC, TimeUnit.MILLISECONDS);

			final Iterator<SearchTask> taskItr = tasks.iterator();

			double completed = 0.0d;
			final double increment = 1.0d / (double) futures.size();
			for (final Future<Long> future : futures) {
				if (canceled)
					throw new InterruptedException("Interrupted by user.");

				final SearchTask task = taskItr.next();
				final String source = task.getURL();
				try {
					resultMap.put(source, future.get(SEARCH_TIMEOUT_MSEC, TimeUnit.MILLISECONDS));
					logger.debug(source + " : Got response = " + resultMap.get(source));
				} catch (ExecutionException e) {
					logger.warn("Error occured in search: " + source, e);
					resultMap.put(source, ERROR_SEARCH_FAILED);
					continue;
				} catch (CancellationException ce) {
					resultMap.put(source, ERROR_CANCEL);
					logger.warn("Operation canceled for " + source, ce);
					continue;
				} catch (TimeoutException te) {
					resultMap.put(source, ERROR_TIMEOUT);
					logger.warn("Operation timeout for " + source, te);
					continue;
				}
				completed += increment;
				tm.setProgress(completed);
			}

		} catch (InterruptedException itEx) {
			// Do some clean up;
			exe.shutdown();
			logger.error("PSICQUIC Search Task interrupted.", itEx);
			resultMap.clear();
			resultMap = null;
			return new ConcurrentHashMap<String, Long>();
		}
		long endTime = System.currentTimeMillis();
		double sec = (endTime - startTime) / (1000.0);
		logger.info("PSICQUIC DB search finished in " + sec + " sec.");

		tm.setProgress(1.0d);

		return resultMap;
	}

	/**
	 * Search each data source and return aggregated result.
	 * 
	 */
	private static final class SearchTask implements Callable<Long> {
		private final String serviceURL;
		private final String query;
		private final SearchMode mode;

		private SearchTask(final String serviceURL, final String query, final SearchMode mode) {
			this.serviceURL = serviceURL;
			this.query = query;
			this.mode = mode;
		}

		public Long call() throws Exception {
			final PSICQUICSimpleClient simpleClient = new PSICQUICSimpleClient(serviceURL);
			if (mode == SearchMode.INTERACTOR)
				return simpleClient.countByInteractor(query);
			else
				return simpleClient.countByQuery(query);
		}

		String getURL() {
			return serviceURL;
		}
	}

	private static final class ImportNetworkAsMitabTask implements Callable<Collection<BinaryInteraction>> {
		private final String serviceURL;
		private final String query;
		private final SearchMode mode;

		private ImportNetworkAsMitabTask(final String serviceURL, final String query, final SearchMode mode) {
			this.serviceURL = serviceURL;
			this.query = query;
			this.mode = mode;
		}

		@Override
		public Collection<BinaryInteraction> call() throws Exception {

			String encodedStr = URLEncoder.encode(query, "UTF-8");
			encodedStr = encodedStr.replaceAll("\\+", "%20");

			URL queryURL = null;
			if (mode == SearchMode.INTERACTOR) {
				// Query is list of interactors.
				queryURL = new URL(serviceURL + "interactor/" + encodedStr);
			} else if (mode == SearchMode.MIQL) {
				queryURL = new URL(serviceURL + "query/" + encodedStr);
			}

			if (queryURL == null)
				throw new IllegalArgumentException("Could not create query URL.");

			logger.info("Query URL: " + queryURL);

			final PsimiTabReader mitabReader = new PsimiTabReader(false);
			return mitabReader.read(queryURL);
		}
		
		public void cancel() {
			canceled = true;
		}
	}

	public void cancel() {
		canceled = true;
	}
}