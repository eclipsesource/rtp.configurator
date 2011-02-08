@echo off
::##############################################################################
:: Copyright (c) 2011 EclipseSource Inc. and others.
:: All rights reserved. This program and the accompanying materials
:: are made available under the terms of the Eclipse Public License v1.0
:: which accompanies this distribution, and is available at
:: http://www.eclipse.org/legal/epl-v10.html
:: 
:: Contributors:
::     Intalio - initial API and implementation
::     EclipseSource - ongoing development
::##############################################################################

:: set path to eclipse folder. If local folder, use '.'; otherwise, use c:\path\to\eclipse
set ECLIPSEHOME=.
 
:: get path to equinox jar inside ECLIPSEHOME folder
for /f "delims= tokens=1" %%c in ('dir /B /S /OD %ECLIPSEHOME%\plugins\org.eclipse.equinox.launcher_*.jar') do set EQUINOXJAR=%%c
 
:: start Eclipse w/ java
echo Using %EQUINOXJAR% to start up Eclipse...
java -jar %EQUINOXJAR% -console %*
