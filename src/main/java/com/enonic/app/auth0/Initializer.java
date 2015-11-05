package com.enonic.app.auth0;

import java.util.concurrent.Callable;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.export.ExportService;
import com.enonic.xp.export.ImportNodesParams;
import com.enonic.xp.export.NodeImportResult;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.PrincipalKeys;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.SecurityService;
import com.enonic.xp.security.User;
import com.enonic.xp.security.UserStoreKey;
import com.enonic.xp.security.auth.AuthenticationInfo;
import com.enonic.xp.vfs.VirtualFile;
import com.enonic.xp.vfs.VirtualFiles;


@Component(immediate = true)
public class Initializer
{
    private ContentService contentService;

    private ExportService exportService;

    private SecurityService securityService;

    private final Logger LOG = LoggerFactory.getLogger( Initializer.class );

    private static final PrincipalKey SUPER_USER_KEY = PrincipalKey.ofUser( UserStoreKey.system(), "su" );

    @Activate
    public void initialize()
        throws Exception
    {
        runAsSuperUser( () -> {
            doInitialize();
            return null;
        } );
    }

    private void doInitialize()
        throws Exception
    {
        final ContentPath demoSitePath = ContentPath.from( "/auth0" );
        if ( hasContent( demoSitePath ) )
        {
            return;
        }

        final Bundle bundle = FrameworkUtil.getBundle( this.getClass() );

        final VirtualFile source = VirtualFiles.from( bundle, "/import" );

        final NodeImportResult nodeImportResult = this.exportService.importNodes( ImportNodesParams.create().
            source( source ).
            targetNodePath( NodePath.create( "/content" ).build() ).
            includeNodeIds( true ).
            includePermissions( true ).
            dryRun( false ).
            build() );

        logImport( nodeImportResult );
    }

    private void logImport( final NodeImportResult nodeImportResult )
    {
        LOG.info( "-------------------" );
        LOG.info( "Imported nodes:" );
        for ( final NodePath nodePath : nodeImportResult.getAddedNodes() )
        {
            LOG.info( nodePath.toString() );
        }

        LOG.info( "-------------------" );
        LOG.info( "Binaries:" );
        nodeImportResult.getExportedBinaries().forEach( LOG::info );

        LOG.info( "-------------------" );
        LOG.info( "Errors:" );
        for ( final NodeImportResult.ImportError importError : nodeImportResult.getImportErrors() )
        {
            LOG.info( importError.getMessage(), importError.getException() );
        }
    }

    private boolean hasContent( final ContentPath path )
    {
        try
        {
            return this.contentService.getByPath( path ) != null;
        }
        catch ( final Exception e )
        {
            return false;
        }
    }

    @Reference
    public void setExportService( final ExportService exportService )
    {
        this.exportService = exportService;
    }

    @Reference
    public void setContentService( final ContentService contentService )
    {
        this.contentService = contentService;
    }

    @Reference
    public void setSecurityService( final SecurityService securityService )
    {
        this.securityService = securityService;
    }

    private <T> T runAsSuperUser( final Callable<T> runnable )
    {
        final AuthenticationInfo authInfo = getSuperUserAuthInfo();
        return ContextBuilder.from( ContextAccessor.current() ).authInfo( authInfo ).build().callWith( runnable );
    }

    private AuthenticationInfo getSuperUserAuthInfo()
    {
        return runAs( RoleKeys.ADMIN, () -> {
            final User superUser = this.securityService.getUser( SUPER_USER_KEY ).
                orElseThrow( () -> new RuntimeException( "User " + SUPER_USER_KEY + " not found." ) );
            final PrincipalKeys principals = this.securityService.getMemberships( SUPER_USER_KEY );
            return AuthenticationInfo.create().principals( principals ).user( superUser ).build();
        } );
    }

    private <T> T runAs( final PrincipalKey role, final Callable<T> runnable )
    {
        final AuthenticationInfo authInfo = AuthenticationInfo.create().principals( role ).user( User.ANONYMOUS ).build();
        return ContextBuilder.from( ContextAccessor.current() ).authInfo( authInfo ).build().callWith( runnable );
    }
}
