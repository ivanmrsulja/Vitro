package edu.cornell.mannlib.vitro.webapp.dynapi.components;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.shared.Lock;

import com.fasterxml.jackson.databind.node.ArrayNode;

import edu.cornell.mannlib.vitro.webapp.dynapi.data.DataStore;
import edu.cornell.mannlib.vitro.webapp.dynapi.data.JsonObjectView;
import edu.cornell.mannlib.vitro.webapp.dynapi.data.ModelView;
import edu.cornell.mannlib.vitro.webapp.dynapi.data.SimpleDataView;
import edu.cornell.mannlib.vitro.webapp.utils.configuration.Property;

public class SparqlSelectQuery extends SparqlQuery {

	public static final Log log = LogFactory.getLog(SparqlSelectQuery.class);

	@Property(uri = "https://vivoweb.org/ontology/vitro-dynamic-api#providesParameter")
	public void addOutputParameter(Parameter param) {
		outputParams.add(param);
	}
	
	@Override
	public OperationResult run(DataStore dataStore) {
		if(!isValid(dataStore)) {
			return OperationResult.internalServerError();
		}
		OperationResult result = OperationResult.ok();
		Model queryModel = ModelView.getModel(dataStore, queryModelParam);
		final String preparedQueryString = prepareQuery(dataStore);

		Map<String,ArrayNode> jsonArrays = JsonObjectView.getJsonArrays(outputParams);
		//Map<String, List> singleDimensionalArrays = ArrayView.getSingleDimensionalArrays(providedParams);
		List<String> simpleData = SimpleDataView.getNames(outputParams);

		queryModel.enterCriticalSection(Lock.READ);
		try {
			QueryExecution qexec = QueryExecutionFactory.create(preparedQueryString, queryModel);
			try {
				ResultSet results = qexec.execSelect();
				int i = 1;
				List<String> vars = results.getResultVars();
				log.debug("Query vars: " + String.join(", ", vars));

				while (results.hasNext()) {
					QuerySolution solution = results.nextSolution();
					log.debug("Query solution " + i++);
					JsonObjectView.addFromSolution(dataStore, jsonArrays, vars, solution, outputParams);
					SimpleDataView.addFromSolution(dataStore, simpleData, vars, solution, outputParams);
				}

			} catch (Exception e) {
				log.error(e.getLocalizedMessage());
				e.printStackTrace();
				result = OperationResult.internalServerError();
			} finally {
				qexec.close();
			}
		} catch (Exception e) {
			log.error(e.getLocalizedMessage());
			e.printStackTrace();
			result = OperationResult.internalServerError();
		} finally {
			queryModel.leaveCriticalSection();
		}
		if (!isOutputValid(dataStore)) {
			return OperationResult.internalServerError();
		}
		return result;
	}

}