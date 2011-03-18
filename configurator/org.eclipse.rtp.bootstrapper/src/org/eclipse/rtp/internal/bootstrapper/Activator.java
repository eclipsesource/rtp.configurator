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

import org.eclipse.osgi.framework.console.CommandProvider;
import org.eclipse.osgi.service.resolver.PlatformAdmin;
import org.eclipse.rtp.internal.bootstrapper.InstallCommandProvider.CommandAction;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

public class Activator implements BundleActivator {

	ServiceRegistration fwAdminReg = null;
	ServiceRegistration simpleConfigReg = null;
	ServiceRegistration profileRestorerReg = null;
	ServiceRegistration installCmdProviderRegistration = null;
	StartLevel startLevelService = null;
	PlatformAdmin platformAdminService = null;
	BundleContext fwAdminContext = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext context) throws Exception {

		ServiceReference ref = context.getServiceReference(PackageAdmin.class
				.getName());
		PackageAdmin packageAdmin = null;
		if (ref != null) {
			packageAdmin = (PackageAdmin) context.getService(ref);
		}
		if (packageAdmin == null) {
			System.out.println("Package Admin service is miising!\n");
			throw new IllegalStateException(
					"Faild to start Eclipse RTP bootstrapper: Package Admin Service is missing");
		}

		String message = "Error registering services FrameworkAdmin and PlatformAdmin. "
				+ "Since declarative services are not included in this installation these services are registered explicitly. "
				+ "You may install menually declarative services and their dependancies and try again.";
		try {
			InstallCommandProvider comandProvider = new InstallCommandProvider(
					context);
			installCmdProviderRegistration = context.registerService(
					CommandProvider.class.getName(), comandProvider, null);

			String toInstall = System.getProperty("installIU");
			String[] iuToInstall = toInstall != null ? toInstall.split(",")
					: null;

			String toUninstall = System.getProperty("uninstallIU");
			String[] iuToUninstall = toUninstall != null ? toUninstall
					.split(",") : null;

			String url = System.getProperty("repository");
			String[] urls = url != null ? url.split(",") : null;
			String operation = null;

			if (iuToInstall != null && iuToInstall.length > 0) {
				operation = "installing:" + toInstall;
			}

			if (iuToUninstall != null && iuToUninstall.length > 0) {
				operation = operation != null ? operation
						+ " and uninstalling:" + iuToUninstall
						: "uninstalling:" + iuToUninstall;
			}

			if (operation != null || Boolean.getBoolean("dialogMode")) {
				try {
					if (!Boolean.getBoolean("dialogMode")) {
						System.out.println("BATCH MODE: Automatically "
								+ operation);
					}

					CommandAction installAction = comandProvider.new CommandAction(
							System.getProperty("destination"), urls,
							iuToInstall, iuToUninstall, System.getProperty(
									"profile", "SDKProfile"));

					if (!Boolean.getBoolean("dialogMode")) {
						installAction.execute();
						System.out
								.println("BATCH MODE: Installation finished.");
					} else {
						installAction.execute();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.exit(0);
			}

		} catch (Exception e) {
			System.out.println(message
					+ ((e.getMessage() != null) ? ": " + e.getMessage() : ""));
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		if (fwAdminReg != null) {
			fwAdminReg.unregister();
		}

		if (simpleConfigReg != null) {
			simpleConfigReg.unregister();
		}

		if (installCmdProviderRegistration != null) {
			installCmdProviderRegistration.unregister();
		}
	}

}
