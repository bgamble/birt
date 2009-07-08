/*************************************************************************************
 * Copyright (c) 2004 Actuate Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Actuate Corporation - Initial implementation.
 ************************************************************************************/

package org.eclipse.birt.report.designer.internal.ui.ide.adapters;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.birt.report.designer.core.model.SessionHandleAdapter;
import org.eclipse.birt.report.designer.internal.ui.util.ExceptionHandler;
import org.eclipse.birt.report.designer.internal.ui.util.UIUtil;
import org.eclipse.birt.report.designer.nls.Messages;
import org.eclipse.birt.report.designer.ui.ReportPlugin;
import org.eclipse.birt.report.designer.ui.editors.IReportProvider;
import org.eclipse.birt.report.designer.ui.ide.wizards.SaveReportAsWizard;
import org.eclipse.birt.report.designer.ui.ide.wizards.SaveReportAsWizardDialog;
import org.eclipse.birt.report.model.api.DesignFileException;
import org.eclipse.birt.report.model.api.IModuleOption;
import org.eclipse.birt.report.model.api.ModuleHandle;
import org.eclipse.birt.report.model.api.core.IModuleModel;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

/**
 * IDE ReportProvider This ReportProvider uses IFileEditorInput as report editor
 * input class.
 */
public class IDEFileReportProvider implements IReportProvider
{

	private ModuleHandle model = null;
	private static final String VERSION_MESSAGE = Messages.getString( "TextPropertyDescriptor.Message.Version" ); //$NON-NLS-1$

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.designer.ui.editors.IReportProvider#connect(org.eclipse.birt.report.model.api.ModuleHandle)
	 */
	public void connect( ModuleHandle model )
	{
		this.model = model;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.designer.ui.editors.IReportProvider#getReportModuleHandle(java.lang.Object)
	 */
	public ModuleHandle getReportModuleHandle( Object element )
	{
		return getReportModuleHandle( element, false );

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.designer.ui.editors.IReportProvider#saveReport(org.eclipse.birt.report.model.api.ModuleHandle,
	 *      java.lang.Object, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void saveReport( ModuleHandle moduleHandle, Object element,
			IProgressMonitor monitor )
	{
		if ( element instanceof IFileEditorInput )
		{
			IFileEditorInput input = (IFileEditorInput) element;
			saveFile( moduleHandle, input.getFile( ), monitor );
		}
		else if ( element instanceof IEditorInput )
		{
			IPath path = getInputPath( (IEditorInput) element );
			if ( path != null )
			{
				saveFile( moduleHandle, path.toFile( ), monitor );
			}
		}

	}

	/**
	 * Save content to a java.io.File
	 * 
	 * @param moduleHandle
	 * @param file
	 * @param monitor
	 */
	private void saveFile( final ModuleHandle moduleHandle, final File file,
			IProgressMonitor monitor )
	{
		if ( file.exists( ) && !file.canWrite( ) )
		{
			MessageDialog.openError( UIUtil.getDefaultShell( ),
					Messages.getString( "IDEFileReportProvider.ReadOnlyEncounter.Title" ), //$NON-NLS-1$
					Messages.getFormattedString( "IDEFileReportProvider.ReadOnlyEncounter.Message", //$NON-NLS-1$
							new Object[]{
								file.getAbsolutePath( )
							} ) );
			return;
		}

		IRunnableWithProgress op = new IRunnableWithProgress( ) {

			public synchronized final void run( IProgressMonitor monitor )
					throws InvocationTargetException, InterruptedException
			{
				try
				{
					IWorkspaceRunnable workspaceRunnable = new IWorkspaceRunnable( ) {

						public void run( IProgressMonitor pm )
								throws CoreException
						{
							try
							{
								execute( pm );
							}
							catch ( CoreException e )
							{
								throw e;
							}
							catch ( IOException e )
							{
								ExceptionHandler.handle( e );
							}
						}
					};

					ResourcesPlugin.getWorkspace( ).run( workspaceRunnable,
							ResourcesPlugin.getWorkspace( ).getRoot( ),
							IResource.NONE,
							monitor );
				}
				catch ( CoreException e )
				{
					throw new InvocationTargetException( e );
				}
				catch ( OperationCanceledException e )
				{
					throw new InterruptedException( e.getMessage( ) );
				}
			}

			public void execute( final IProgressMonitor monitor )
					throws CoreException, IOException
			{
				if ( file.exists( ) || file.createNewFile( ) )
				{
					OutputStream out = null;
					try
					{
						out = new BufferedOutputStream(new FileOutputStream( file ), 8192*2);
						moduleHandle.serialize( out );
						out.flush( );
					}
					catch ( FileNotFoundException e )
					{
					}
					catch ( IOException e )
					{
					}
					finally
					{
						if ( out != null )
							out.close( );
					}
				}
			}
		};

		try
		{
			new ProgressMonitorDialog( UIUtil.getDefaultShell( ) ).run( false,
					true,
					op );
		}

		catch ( Exception e )
		{
			ExceptionHandler.handle( e );
		}
	}

	/**
	 * Save content to workspace file.
	 * 
	 * @param moduleHandle
	 * @param file
	 * @param monitor
	 */
	private void saveFile( final ModuleHandle moduleHandle, final IFile file,
			IProgressMonitor monitor )
	{
		if ( file.exists( ) && file.isReadOnly( ) )
		{
			MessageDialog.openError( UIUtil.getDefaultShell( ),
					Messages.getString( "IDEFileReportProvider.ReadOnlyEncounter.Title" ), //$NON-NLS-1$
					Messages.getFormattedString( "IDEFileReportProvider.ReadOnlyEncounter.Message", //$NON-NLS-1$
							new Object[]{
								file.getFullPath( )
							} ) );
			return;
		}

		IRunnableWithProgress op = new IRunnableWithProgress( ) {

			public synchronized final void run( IProgressMonitor monitor )
					throws InvocationTargetException, InterruptedException
			{
				try
				{
					IWorkspaceRunnable workspaceRunnable = new IWorkspaceRunnable( ) {

						public void run( IProgressMonitor pm )
								throws CoreException
						{
							try
							{
								execute( pm );
							}
							catch ( CoreException e )
							{
								throw e;
							}
							catch ( IOException e )
							{
								ExceptionHandler.handle( e );
							}
						}
					};

					ResourcesPlugin.getWorkspace( ).run( workspaceRunnable,
							ResourcesPlugin.getWorkspace( ).getRoot( ),
							IResource.NONE,
							monitor );
				}
				catch ( CoreException e )
				{
					throw new InvocationTargetException( e );
				}
				catch ( OperationCanceledException e )
				{
					throw new InterruptedException( e.getMessage( ) );
				}
			}

			public void execute( final IProgressMonitor monitor )
					throws CoreException, IOException
			{

				ByteArrayOutputStream out = new ByteArrayOutputStream( );
				moduleHandle.serialize( out );
				byte[] bytes = out.toByteArray( );
				out.close( );

				ByteArrayInputStream is = new ByteArrayInputStream( bytes );

				IContainer container = file.getParent( );
				if ( !container.exists( ) && container instanceof IFolder )
				{
					UIUtil.createFolder( (IFolder) container, monitor );
				}

				if ( file.exists( ) )
				{
					file.setContents( is, true, true, monitor );
				}
				else
				{
					// Save to new file.
					file.create( is, true, monitor );
				}
			}
		};

		try
		{
			new ProgressMonitorDialog( UIUtil.getDefaultShell( ) ).run( false,
					true,
					op );
		}
		catch ( Exception e )
		{
			ExceptionHandler.handle( e );
		}

		try
		{
			file.refreshLocal( 0, monitor );
		}
		catch ( CoreException e )
		{
			ExceptionHandler.handle( e );
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.designer.ui.editors.IReportProvider#getSaveAsPath(java.lang.Object)
	 */
	public IPath getSaveAsPath( Object element )
	{
		IFile file = null;
		if ( element instanceof IFileEditorInput )
		{
			IFileEditorInput input = (IFileEditorInput) element;
			file = input.getFile( );
		}
		SaveReportAsWizardDialog dialog = new SaveReportAsWizardDialog( UIUtil.getDefaultShell( ),
				new SaveReportAsWizard( model, file ) );
		if ( dialog.open( ) == Window.OK )
		{
			return dialog.getResult( );
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.designer.ui.editors.IReportProvider#createNewEditorInput(org.eclipse.core.runtime.IPath)
	 */
	public IEditorInput createNewEditorInput( IPath path )
	{
		return new FileEditorInput( ResourcesPlugin.getWorkspace( )
				.getRoot( )
				.getFile( path ) );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.designer.ui.editors.IReportProvider#getInputPath(org.eclipse.ui.IEditorInput)
	 */
	public IPath getInputPath( IEditorInput input )
	{
		if ( input instanceof FileEditorInput )
		{
			return ( (FileEditorInput) input ).getPath( );
		}
		else if ( input instanceof IURIEditorInput )
		{
			return new Path( ( (IURIEditorInput) input ).getURI( ).getPath( ) );
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.designer.ui.editors.IReportProvider#getReportDocumentProvider(java.lang.Object)
	 */
	public IDocumentProvider getReportDocumentProvider( Object element )
	{
		if ( element instanceof FileEditorInput )
		{
			// workspace file
			return new ReportDocumentProvider( );
		}
		else
		{
			// system file
			return new IDEFileReportDocumentProvider( );
		}
	}

	public ModuleHandle getReportModuleHandle( Object element, boolean reset )
	{
		if ( model == null || reset )
		{
			IEditorInput input = (IEditorInput) element;
			IPath path = getInputPath( input );
			if ( path != null )
			{
				String fileName = path.toOSString( );
				try
				{
					Map properties = new HashMap( );

					String designerVersion = MessageFormat.format( VERSION_MESSAGE,
							new String[]{
									ReportPlugin.getVersion( ),
									ReportPlugin.getBuildInfo( )
							} );
					properties.put( IModuleModel.CREATED_BY_PROP,
							designerVersion );
					properties.put( IModuleOption.CREATED_BY_KEY,
							designerVersion );
					String projectFolder = getProjectFolder( input );
					if ( projectFolder != null )
					{
						properties.put( IModuleOption.RESOURCE_FOLDER_KEY,
								projectFolder );
					}
					model = SessionHandleAdapter.getInstance( ).init( fileName,
							new FileInputStream( path.toFile( ) ),
							properties );
				}
				catch ( DesignFileException e )
				{
					// not safe pop up a dialog here, just log it.
					ExceptionHandler.handle( e, true );
				}
				catch ( IOException e )
				{
					// not safe pop up a dialog here, just log it.
					ExceptionHandler.handle( e, true );
				}
			}
		}
		return model;
	}

	private String getProjectFolder( IEditorInput input )
	{
		Object fileAdapter = input.getAdapter( IFile.class );
		IFile file = null;
		if ( fileAdapter != null )
			file = (IFile) fileAdapter;
		if ( file != null && file.getProject( ) != null )
		{
			return file.getProject( ).getLocation( ).toOSString( );
		}
		IPath path = getInputPath( input );
		if ( path != null )
		{
			return path.toFile( ).getParent( );
		}
		return null;
	}

}
