/******************************************************************************* 
 * Copyright (c) 2011 SAP AG and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.rtp.internal.bootstrapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.metadata.expression.IExpression;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;

/**
 * This class provides functionality for listing the contents of a list of p2 repositories.
 * For each repository are listed the category elements, and for each category element - the 
 * provided features / bundles.
 * 
 * The categories in a repository are determined by the following algorithm:
 * 
 * 1. Query the repository for all category elements
 * 
 * 2. Merge some categories from the list if needed (whether two categories should be merged 
 *    is determined by comparing the values of the org.eclipse.equinox.p2.name property, or 
 *    the unit id, if the property is not set). The merged categories are represented by one 
 *    of them, but the requirements of the resulting merged category includes the requirements
 *    of all of them.
 * 
 * 3. Check if in the list, resulting from the merge, there are categories, which are 
 *    referenced (required) by other categories. If there are such categories, they are removed
 *    from the list.
 *    
 * 4. For each category in the resulting list retrieve the provided features / bundles
 * 
 * @author I043832
 *
 */
public class RepositoryHandler {

	
	
	
	public RepositoryHandler() {
	}
	
	public void listRepositories(ProvisioningSession session, ProvisioningContext context) {

		IQueryable<IInstallableUnit> query = context.getMetadata(new NullProgressMonitor());
			ArrayList<CategoryElement> uniqueCategories = retrieveCategories(query);
			Iterator<CategoryElement> categoriesIterator = uniqueCategories.iterator();
			while(categoriesIterator.hasNext()) {
				CategoryElement category = categoriesIterator.next();
				IQueryResult<IInstallableUnit> result = retrieveCategoryMembers(query, category);
				printCategories(result, category);
			}

	}
	
	
	/*
	 * This method retrieves all categories from a repository, eventually merges some of them
	 * and removes those, which are referenced by other categories.
	 */
	protected ArrayList<CategoryElement> retrieveCategories(IQueryable<IInstallableUnit> query) {
		IQuery<IInstallableUnit> categoryQuery = QueryUtil.createIUCategoryQuery();
		IQueryResult<IInstallableUnit> result = query.query(categoryQuery, null);
		ArrayList<CategoryElement> uniqueCategories = new ArrayList<CategoryElement>();
		Iterator<IInstallableUnit> iterator = result.iterator();
		Set<String> referredIUs = new HashSet<String>();
		
		while(iterator.hasNext()) {
			IInstallableUnit unitToTest = iterator.next();
			Iterator<CategoryElement> iter = uniqueCategories.iterator();
			boolean isUnique = true;
			
			for (IRequirement requirement : unitToTest.getRequirements()) {
				if (requirement instanceof IRequiredCapability) {
					if (((IRequiredCapability) requirement).getNamespace().equals(IInstallableUnit.NAMESPACE_IU_ID)) {
						referredIUs.add(((IRequiredCapability) requirement).getName());
					}
				}
			}
			
			// check for categories which should be merged 
			while(iter.hasNext()) {
				CategoryElement element = iter.next();
				IInstallableUnit unit = element.getIU();
				
				if(getComparisonKey(unit).equals(getComparisonKey(unitToTest))) {
					element.mergeIU(unitToTest);
					isUnique = false;
					break;
				}
			}
			if(isUnique == true) {
				uniqueCategories.add(new CategoryElement(unitToTest));
			}
		}
		
		// remove categories, referenced by others
		CategoryElement[] categoryIUs = uniqueCategories.toArray(new CategoryElement[uniqueCategories.size()]);
		for(CategoryElement categoryIU : categoryIUs) {
			if(referredIUs.contains(categoryIU.getIU().getId())) {
				uniqueCategories.remove(categoryIU);
			}
		}
		
		return uniqueCategories;
	}
	
	protected String getComparisonKey(IInstallableUnit iu) {
		String name = iu.getProperty("org.eclipse.equinox.p2.name", null);
		if(name == null || name.length() == 0) {
			name = iu.getId();
		}
		return name;
	}
	
	protected void printCategories(IQueryResult<IInstallableUnit> result, CategoryElement category) {
		Iterator<IInstallableUnit> iterator = result.iterator();
		
		System.out.println();
		System.out.print("Category: " + category.getIU().getProperty("org.eclipse.equinox.p2.name", null) + " - ");
		for(IInstallableUnit iu : category.getIus()) {
			System.out.print(iu.getId() + " " + iu.getVersion() + "\t");
		}
		
		System.out.println();
		System.out.println("Contained units: ");
		while(iterator.hasNext()) {
			IInstallableUnit iu = iterator.next();
			System.out.println("\t" + iu.getProperty("org.eclipse.equinox.p2.name", null) + " - " + iu.getId());
		}
		
		System.out.println();
	}
	
	protected IQueryResult<IInstallableUnit> retrieveCategoryMembers(IQueryable<IInstallableUnit> query, CategoryElement category) {
		IExpression matchesRequirementsExpression = ExpressionUtil.parse("$0.exists(r | this ~= r)"); 
		IQuery<IInstallableUnit> memberOfCategoryQuery = QueryUtil.createMatchQuery(matchesRequirementsExpression, category.getRequirements());
		memberOfCategoryQuery = QueryUtil.createLatestQuery(memberOfCategoryQuery);
		IQueryResult<IInstallableUnit> result = query.query(memberOfCategoryQuery, null);
		return result;
	}
}
