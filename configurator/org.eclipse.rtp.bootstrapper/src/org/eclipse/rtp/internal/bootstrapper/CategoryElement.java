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
import java.util.Collection;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;

/**
 * This class represents a category element. It may include several installable units, which have
 * one and the same value for the org.eclipse.equinox.p2.name property, or one and the same unit id,
 * if the property is not set.
 * 
 *
 */
public class CategoryElement {
	private ArrayList<IInstallableUnit> ius = new ArrayList<IInstallableUnit>();
	private Collection<IRequirement> requirements = null;
	
	public CategoryElement(IInstallableUnit iu) {
		ius.add(iu);
	}
	
	public IInstallableUnit getIU() {
		if(ius == null || ius.isEmpty()) {
			return null;
		} else {
			return ius.get(0);
		}
	}
	
	public Collection<IInstallableUnit> getIus() {
		return ius;
	}
	
	public Collection<IRequirement> getRequirements() {
		if(ius == null || ius.isEmpty()) {
			return null;
		} 
		
		if(requirements == null) {
			if(ius.size() == 1) {
				requirements = ius.get(0).getRequirements();	
			} else {
				ArrayList<IRequirement> reqs = new ArrayList<IRequirement>();
				for(IInstallableUnit iu : ius) {
					reqs.addAll(iu.getRequirements());
				}
				requirements = reqs;
			}
		}
		
		return requirements;
	}
	
	public void mergeIU(IInstallableUnit iu) {
		ius.add(iu);
	}
}
