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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.UninstallOperation;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class InstallCommandProvider implements CommandProvider {
	private BundleContext context;
	private static String INSTALL_FOLDER_OPTION = "-destination";
	private static String INSTALL_FOLDER_OPTION_ABBR = "-dest";
	private static String REPO_URL_OPTION = "-repository";
	private static String REPO_URL_OPTION_ABBR = "-repo";
	private static String BUNDLE_TO_INSTALL_OPTION = "-installIU";
	private static String BUNDLE_TO_INSTALL_OPTION_ABBR = "-iu";
	private static String PROFILE_TO_INSTALL_OPTION = "-profile";
	private static String BUNDLE_TO_UNINSTALL_OPTION = "-uninstallIU";
	private static String P2_PROPERTIES_FILE_OPTION = "-properties";
	private static String P2_PROPERTIES_FILE_OPTION_ABBR = "-props";
	private static String DEBUG_OPTION = "-updater_debug";
	
	private boolean isDebug = false; 

	InstallCommandProvider(BundleContext context) {
		this.context = context;
	}

	public void _p2_install(CommandInterpreter intp) {
		CommandAction action = new CommandAction();
		action.execute(intp);		
    }

	public String getHelp() {
		StringBuilder help = new StringBuilder();
		help.append("---");
		help.append("P2 installer commands");
		help.append("---");
		help.append("\r\n");
		help.append("\t");
		help.append("p2_install - installs a unit and its dependencies");
		help.append("\r\n");
		help.append("\t\t");
		help.append("parameters:");
		help.append("\r\n");
		help.append("\t\t");
		help.append("-installIU | -iu - unit to install");
		help.append("\r\n");
		help.append("\t\t");
		help.append("-uninstallIU - unit to uninstall");
		help.append("\r\n");
		help.append("\t\t");
		help.append("-destination | -dest - folder in which to install the unit");
		help.append("\r\n");
		help.append("\t\t");
		help.append("-repository | -repo - comma separated list of repositories to be looked up for the bundle to install");
		help.append("\r\n");
		help.append("\t\t");
		help.append("-properties | -props - file which contains p2 properties for instrumenting the provisioning process; if not specified, the properties will be looked for in a p2.properties file in the current folder");
		help.append("\r\n");
		help.append("\t\t");
		help.append("-profile - profile id containing the description of the targeted product. Default is SDKProfile");
		help.append("\r\n");
		help.append("\t\t");
		help.append("-updater_debug - turns on detailed error info");
		help.append("\r\n");
		return help.toString();
	}

	
	private static IProvisioningAgent getAgent(BundleContext context, String folder) throws ProvisionException, URISyntaxException {
		IProvisioningAgentProvider provider = (IProvisioningAgentProvider) getService(context, IProvisioningAgentProvider.SERVICE_NAME); 
		return provider.createAgent((new File(folder+"/p2")).toURI()); 
	}


	public static Object getService(BundleContext context, String name) {
		if (context == null)
			return null;
		ServiceReference reference = context.getServiceReference(name);
		if (reference == null)
			return null;
		Object result = context.getService(reference);
		context.ungetService(reference);
		return result;
	}
	
	protected class CommandAction {
		private String installFolder;
		private String[] repoUrls;
		private String[] unitToInstall = null;
		private String[] unitToUninstall = null;
		private IProvisioningAgent agent;
		private String profileId = "SDKProfile";
		private String p2PropertiesFileLocation;
		
		
		public CommandAction() {			
		}
		 
		
		public CommandAction(String installFolder, String[] repoUrls,
				String[] unitToInstall, String[] unitToUninstall, String profileId) {
			super();
			this.installFolder = installFolder;
			this.repoUrls = repoUrls;
			this.unitToInstall = unitToInstall;
			this.unitToUninstall = unitToUninstall;
			this.profileId = profileId;
		}

		public void execute(CommandInterpreter intp) {
			String arg = intp.nextArgument();
			while (arg != null) {
				if (arg.equalsIgnoreCase(INSTALL_FOLDER_OPTION) || arg.equalsIgnoreCase(INSTALL_FOLDER_OPTION_ABBR)) {
					installFolder = intp.nextArgument();
				} else if (arg.equalsIgnoreCase(REPO_URL_OPTION) || arg.equalsIgnoreCase(REPO_URL_OPTION_ABBR)) {
					String repos = intp.nextArgument();
					if (repos != null) {
						repoUrls = repos.split(",");
						if (repoUrls.length == 0) repoUrls = new String[] {repos};
					}
				} else if (arg.equalsIgnoreCase(BUNDLE_TO_INSTALL_OPTION) || arg.equalsIgnoreCase(BUNDLE_TO_INSTALL_OPTION_ABBR)) {
					String toInstall = intp.nextArgument();
					if (toInstall != null) {
						//try to split the IUs - if not possible save the whole string as it is regarded as a single IU
						unitToInstall = toInstall.split(",");
						if (unitToInstall.length == 0) unitToInstall = new String[] {toInstall};
					} else {
						//if there is no argument specified for install save null
						unitToInstall = new String[] {null};
					}
				} else if (arg.equalsIgnoreCase(BUNDLE_TO_UNINSTALL_OPTION)) {
					String toUninstall = intp.nextArgument();
					if (toUninstall != null) {
						//try to split the IUs - if not possible save the whole string as it is regarded as a single IU
						unitToUninstall = toUninstall.split(",");
						if (unitToUninstall.length == 0) unitToUninstall = new String[] {toUninstall};
					} else {
						//if there is no argument specified for uninstall save null
						unitToUninstall = new String[] {null};
					}
				} else if (arg.equalsIgnoreCase(DEBUG_OPTION)) {
					isDebug = true;
				} else if (arg.equalsIgnoreCase(PROFILE_TO_INSTALL_OPTION)) {
					profileId = intp.nextArgument();
				} else if (arg.equalsIgnoreCase(P2_PROPERTIES_FILE_OPTION) || arg.equalsIgnoreCase(P2_PROPERTIES_FILE_OPTION_ABBR)) {
					p2PropertiesFileLocation = intp.nextArgument();
				}

				arg = intp.nextArgument();
			}
			try {
				execute();
			} catch (Exception e) {
				System.out.println("Error during command execution");
				e.printStackTrace();
			}
		}
		
		public void execute() throws Exception {
			if (repoUrls == null || (repoUrls != null && repoUrls.length == 0)) {
				System.out.println("Repository location not specified. An example of a correct update site is http://download.eclipse.org/eclipse/updates/3.6milestones/");
				System.out.println("\n" + getHelp());				
				throw new Exception("Repository location not specified");
			}

			if (installFolder == null || "".equals(installFolder)) {
				installFolder = ".";
			}

			if (isDebug == false) {
				if (System.getProperty("updater_debug") != null) {
					isDebug = true;
				}
			}
			
			try{
				agent = getAgent(context, installFolder);
			} catch (Exception e) {
				if (isDebug == true) {
					e.printStackTrace();
				}
				throw new Exception("Error getting provisioning agent: " + ((e.getMessage() != null) ? ": " + e.getMessage() : ""));
			} 
			
			ProvisioningSession session = new ProvisioningSession(agent);
			// set provisioning context: get it as a System property repoURI
			ProvisioningContext provisionContext = getProvisioningContext(context);
			
			if (unitToInstall != null && unitToInstall[0] == null) {
				System.out.println("Bundle for installing not specified");
				RepositoryHandler repoHandler = new RepositoryHandler();
				repoHandler.listRepositories(session, provisionContext);
				System.out.println("\n" + getHelp());				
				throw new Exception("Bundle for installing not specified");
			}
			
			if (unitToUninstall != null && unitToUninstall[0] == null) {
				System.out.println("Bundle for uninstalling not specified");
				System.out.println("\n" + getHelp());	
				throw new Exception("Bundle for uninstalling not specified");
			}			

			if (unitToInstall != null && unitToInstall.length != 0) {
				StringBuffer willInstall = new StringBuffer();
				for (String s : unitToInstall) {
					willInstall.append(" ").append(s);
				}
				System.out.println("Installing bundle " + willInstall + " ...");
				Collection<IInstallableUnit> forInstall = getIInstallableUnits(provisionContext, unitToInstall);
				InstallOperation install = new InstallOperation(session, forInstall);
				install.setProfileId(profileId);
				install.setProvisioningContext(provisionContext);
				IStatus result = install.resolveModal(new CmdPromtProgressMonitor());
				 if (result.isOK()) {
					 install.getProvisioningJob(new CmdPromtProgressMonitor()).runModal(new CmdPromtProgressMonitor());
				 }
			}

			StringBuffer willUnInstall = new StringBuffer();
			if (unitToUninstall != null && unitToUninstall.length != 0) {
				for (String s : unitToUninstall) {
					willUnInstall.append(" ").append(s);
				}
				System.out.println("Uninstalling bundle " + willUnInstall + " ...");
				Collection<IInstallableUnit> forInstall = getIInstallableUnits(provisionContext, unitToInstall);
				UninstallOperation unInstall = new UninstallOperation(session, forInstall);
				unInstall.setProfileId(profileId);
				unInstall.setProvisioningContext(provisionContext);
				IStatus result = unInstall.resolveModal(new CmdPromtProgressMonitor());
				 if (result.isOK()) {
					 unInstall.getProvisioningJob(new CmdPromtProgressMonitor()).runModal(new CmdPromtProgressMonitor());
				 }
			}									
			
			

			
			System.out.println("Operation finished successfully");

		}
		
		
		private ProvisioningContext getProvisioningContext(BundleContext context) throws Exception{
			
			URI[] repoURIs = new URI [repoUrls.length];
			for(int i = 0; i < repoUrls.length; i++) {
				//when creating the URI, handle Windows file paths with backward slashes
				try {
					repoURIs[i] = new URI(repoUrls[i].replace("\\", "/"));
				} catch (URISyntaxException e){
					if (isDebug == true) {
						e.printStackTrace();
					}
					throw new Exception("The entered repository URI " + repoURIs[i] + " is not valid");
				}
			}
			ProvisioningContext result = new ProvisioningContext(agent);
			result.setArtifactRepositories(repoURIs);
			result.setMetadataRepositories(repoURIs);
			return result;

		}		
		
		private Collection<IInstallableUnit> getIInstallableUnits(ProvisioningContext provisioningContext,String[] unitNames) {
			HashSet<IInstallableUnit> units = new HashSet<IInstallableUnit>();
			for (String name : unitNames ) {
				IInstallableUnit allIUFromRepo = getNewInstallableUnitById(provisioningContext,name);
				units.add(allIUFromRepo);
			}			
			return units;		
		}		
		

		private IInstallableUnit getNewInstallableUnitById(ProvisioningContext provisioningContext, String id){

			for(IInstallableUnit iu : provisioningContext.getExtraInstallableUnits()){
				for(IProvidedCapability pc : iu.getProvidedCapabilities()){
					if (id.equals(pc.getName())){
						iu.getTouchpointData();
						iu.getMetaRequirements();

						return iu;

					}
				}

			}

			// if there are no Installable Units in the ExtraInstallable Units, will try to get the IU from the metadata

			IQueryable<IInstallableUnit> queryable = provisioningContext.getMetadata(new NullProgressMonitor());
			IQueryResult<IInstallableUnit> matches = queryable.query(QueryUtil.createLatestIUQuery(),new NullProgressMonitor());
			for (Iterator<IInstallableUnit> it = matches.iterator(); it.hasNext();) {
				IInstallableUnit iu = it.next();		

				if (iu.getId().equals(id)) {
					return iu;
				}

			}
			return null; 
		}

	}

}
