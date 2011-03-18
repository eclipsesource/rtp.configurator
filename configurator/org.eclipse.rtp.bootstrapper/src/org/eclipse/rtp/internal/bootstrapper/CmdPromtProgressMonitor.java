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

import org.eclipse.core.runtime.IProgressMonitor;

public class CmdPromtProgressMonitor implements IProgressMonitor {

	private static String anim = "==========";
	private int x = 0;
	private int totalwork;
	private int done;

	StringBuilder progres = new StringBuilder();

	public CmdPromtProgressMonitor() {
		super();		
	}

	@Override
	public void beginTask(String name, int totalWork) {
		System.out.println();
		this.totalwork = totalWork;

	}

	@Override
	public void done() {
		System.out.println("\n");

	}

	@Override
	public void internalWorked(double work) {
		System.out.print(".");

	}

	@Override
	public boolean isCanceled() {
		return false;
	}

	@Override
	public void setCanceled(boolean value) {
	}

	@Override
	public void setTaskName(String name) {
	}

	@Override
	public void subTask(String name) {
	}

	@Override
	public void worked(int work) {
		x = x + work;
		done = (x * 100) / totalwork;
		for (int i = 0; i < progres.length(); i++) {
			System.out.print("\b");
		}
		progres = new StringBuilder();
		progres.append(anim.substring(0, done / anim.length())).append(done)
				.append("%");
		System.out.print(progres.toString());

	}

}
