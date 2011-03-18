/******************************************************************************* 
 * Copyright (c) 2011 SAP AG and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.rtp.bootstrapper.launch;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class SelfExtractor {

	static String MANIFEST = "META-INF/MANIFEST.MF";
	private String selExtractorClassName;

	private File outDir;
	private File outputDir;

	public static void main(String args[]) {
		parseArguments(args);
		SelfExtractor zse = new SelfExtractor();
		String jarFileName = zse.getJarFileName();
		zse.extract(jarFileName);
		zse.executeInstall(args);
		System.exit(0);
	}

	SelfExtractor() {
	}

	private static void parseArguments(String args[]) {
		for (int i = 0; i < args.length; i++)
			if (args[i].startsWith("-")) {
				if (args.length > i + 1)
					if (args[i].equals("-installIU"))
						System.setProperty("installIU", args[++i]);
					else if (args[i].equals("-destination"))
						System.setProperty("destination", args[++i]);
					else if (args[i].equals("-repository"))
						System.setProperty("repository", args[++i]);
					else if (args[i].equals("-profile"))
						System.setProperty("profile", args[++i]);
			}

	}

	private String getJarFileName() {
		selExtractorClassName = (new StringBuilder(String.valueOf(getClass()
				.getName().replaceAll("\\.", "/")))).append(".class")
				.toString();
		URL urlJar = ClassLoader.getSystemResource(selExtractorClassName);
		String urlStr = urlJar.toString();
		int from = "jar:file:".length();
		int to = urlStr.indexOf("!/");
		return urlStr.substring(from, to);
	}

	public void extract(String zipfile) {

		File currentArchive = new File(zipfile);
		;

		SimpleDateFormat formatter;

		String desFolder = System.getProperty("destination");
		if (desFolder == null || desFolder.length() == 0) {
			System.out.print("\n Please select installation folder: ");
			BufferedReader br = new BufferedReader(new InputStreamReader(
					System.in));
			try {
				desFolder = br.readLine();
			} catch (IOException ioe) {
				System.out
						.println("IO error trying to read installation folder!");
				System.exit(1);
			}

		}

		outDir = new File(desFolder);
		outputDir = new File(outDir, "tempInstall");
		byte buf[] = new byte[1024];
		formatter = new SimpleDateFormat("MM/dd/yyyy hh:mma",
				Locale.getDefault());
		boolean overwrite = false;

		ZipFile zf = null;
		FileOutputStream out = null;
		InputStream in = null;
		int result = 0;

		try {
			zf = new ZipFile(currentArchive);

			int size = zf.size();

			Enumeration<? extends ZipEntry> entries = zf.entries();

			for (int i = 0; i < size; i++) {
				ZipEntry entry = entries.nextElement();
				if (entry.isDirectory())
					continue;

				String pathname = entry.getName();
				if (selExtractorClassName.equals(pathname)
						|| MANIFEST.equals(pathname.toUpperCase()))
					continue;

				in = zf.getInputStream(entry);

				File outFile = new File(outputDir, pathname);
				Date archiveTime = new Date(entry.getTime());

				if (!overwrite) {
					if (outFile.exists()) {

						Date existTime = new Date(outFile.lastModified());
						Long archiveLen = new Long(entry.getSize());

						String msg = "File name conflict: "
								+ "There is already a file with "
								+ "that name on the disk!\n" + "\nFile name: "
								+ outFile.getName() + "\nExisting file: "
								+ formatter.format(existTime) + ",  "
								+ outFile.length() + "Bytes"
								+ "\nFile in archive:"
								+ formatter.format(archiveTime) + ",  "
								+ archiveLen + "Bytes"
								+ "\n\nWould you like to overwrite the file?";

						if (result == 2) // No
						{
							continue;
						} else if (result == 1) // YesToAll
						{
							overwrite = true;
						}
					}
				}

				File parent = new File(outFile.getParent());
				if (parent != null && !parent.exists()) {
					parent.mkdirs();
				}

				out = new FileOutputStream(outFile);

				while (true) {
					int nRead = in.read(buf, 0, buf.length);
					if (nRead <= 0)
						break;
					out.write(buf, 0, nRead);
				}

				out.close();
				outFile.setLastModified(archiveTime.getTime());
			}

			zf.close();

		} catch (Exception e) {
			System.out.println(e);
			if (zf != null) {
				try {
					zf.close();
				} catch (IOException ioe) {
					;
				}
			}
			if (out != null) {
				try {
					out.close();
				} catch (IOException ioe) {
					;
				}
			}
			if (in != null) {
				try {
					in.close();
				} catch (IOException ioe) {
					;
				}
			}
		}

	}

	private void executeInstall(String args[]) {
		try {
			File pluginsFolder = new File(outputDir, "plugins");
			URL url[] = {
					pluginsFolder.toURL(),
					(new File(pluginsFolder,
							"org.eclipse.osgi_3.6.0.v20100517.jar")).toURL() };
			ClassLoader cl = new URLClassLoader(url);
			Class eclipseStrater = cl
					.loadClass("org.eclipse.core.runtime.adaptor.EclipseStarter");
			Class arguments[] = { (new String[0]).getClass() };
			Method main = eclipseStrater.getMethod("main", arguments);
			System.setProperty("osgi.install.area", outputDir.getAbsolutePath());
			System.setProperty("osgi.configuration.area", (new File(outputDir,
					"configuration")).getAbsolutePath());
			if (System.getProperty("p2.install.config") == null)
				System.setProperty(
						"p2.install.config",
						(new StringBuilder(String.valueOf(outputDir.getPath())))
								.append("/p2.properties").toString());
			System.setProperty("destination", outDir.getAbsolutePath());
			System.setProperty("dialogMode", "true");
			Runtime.getRuntime().addShutdownHook(new ShutdownHook(outputDir));
			main.invoke(eclipseStrater, new Object[] { args });
			synchronized (this) {
				wait();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	class ShutdownHook extends Thread {

		File dir;

		public ShutdownHook(File dir) {
			super();
			this.dir = dir;
		}

		public void run() {
			deleteDir(dir);
			System.out.println("Press any key to finish.....");
			try {
				System.in.read();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void deleteDir(File dir) {
			if (dir.isDirectory()) {
				String children[] = dir.list();
				for (int i = 0; i < children.length; i++)
					deleteDir(new File(dir, children[i]));

			}
			dir.deleteOnExit();
		}

	}

}
