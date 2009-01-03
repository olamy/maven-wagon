package org.apache.maven.wagon.providers.webdav;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import it.could.webdav.DAVServlet;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.StreamingWagon;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.http.HttpWagonTestCase;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

/*
 * WebDAV Wagon Test
 * 
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * 
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 */
public class WebDavWagonTest
    extends HttpWagonTestCase
{
    protected String getTestRepositoryUrl()
        throws IOException
    {
        return getProtocol() + "://localhost:10007/newfolder/folder2";
    }

    protected String getProtocol()
    {
        return "dav";
    }

    protected void createContext( Server server, File repositoryDirectory )
        throws IOException
    {
        Context dav = new Context( server, "/", Context.SESSIONS );
        ServletHolder davServletHolder = new ServletHolder( new DAVServlet() );
        davServletHolder.setInitParameter( "rootPath", repositoryDirectory.getAbsolutePath() );
        davServletHolder.setInitParameter( "xmlOnly", "false" );
        dav.addServlet( davServletHolder, "/*" );
    }

    protected long getExpectedLastModifiedOnGet( Repository repository, Resource resource )
    {
        File file = new File( getDavRepository(), resource.getName() );
        return ( file.lastModified() / 1000 ) * 1000;
    }

    
    private File getDavRepository()
    {
        return getTestFile( "target/test-output/http-repository/newfolder/folder2" );
    }

    private void assertURL( String userUrl, String expectedUrl )
    {
        Repository repo = new Repository( "test-geturl", userUrl );
        String actualUrl = ( new WebDavWagon() ).getURL( repo );
        assertEquals( "WebDavWagon.getURL(" + userUrl + ")", expectedUrl, actualUrl );
    }

    /**
     * Tests the maven 2.0.x way to define a webdav URL without SSL.
     */
    public void testGetURLDavHttp()
    {
        assertURL( "dav:http://localhost:10007/dav/", "http://localhost:10007/dav/" );
    }

    /**
     * Tests the maven 2.0.x way to define a webdav URL with SSL.
     */
    public void testGetURLDavHttps()
    {
        assertURL( "dav:https://localhost:10007/dav/", "https://localhost:10007/dav/" );
    }

    /**
     * Tests the URI spec way of defining a webdav URL without SSL.
     */
    public void testGetURLDavUri()
    {
        assertURL( "dav://localhost:10007/dav/", "http://localhost:10007/dav/" );
    }

    /**
     * Tests the URI spec way of defining a webdav URL with SSL.
     */
    public void testGetURLDavUriWithSsl()
    {
        assertURL( "davs://localhost:10007/dav/", "https://localhost:10007/dav/" );
    }

    /**
     * Tests the URI spec way of defining a webdav URL without SSL.
     */
    public void testGetURLDavPlusHttp()
    {
        assertURL( "dav+https://localhost:10007/dav/", "https://localhost:10007/dav/" );
    }

    /**
     * Tests the URI spec way of defining a webdav URL with SSL.
     */
    public void testGetURLDavPlusHttps()
    {
        assertURL( "dav+https://localhost:10007/dav/", "https://localhost:10007/dav/" );
    }

    protected void setHttpHeaders( StreamingWagon wagon, Properties properties )
    {
        ( (WebDavWagon) wagon ).setHttpHeaders( properties );
    }

    /**
     * Make sure wagon webdav can detect remote directory
     * @throws Exception
     */
    public void testWagonWebDavGetFileList()
        throws Exception
    {
        setupRepositories();

        setupWagonTestingFixtures();

        String dirName = "file-list";

        String filenames[] = new String[] {
            "test-resource.txt",
            "test-resource.pom",
            "test-resource b.txt",
            "more-resources.dat" };

        for ( int i = 0; i < filenames.length; i++ )
        {
            putFile( dirName + "/" + filenames[i], dirName + "/" + filenames[i], filenames[i] + "\n" );
        }

        String dirnames[] = new String[] {
            "test-dir1",
            "test-dir2"};

        for ( int i = 0; i < dirnames.length; i++ )
        {
            new File( getDavRepository(), dirName + "/" + dirnames[i] ).mkdirs();
        }

        Wagon wagon = getWagon();

        wagon.connect( testRepository, getAuthInfo() );

        List list = wagon.getFileList( dirName );

        assertNotNull( "file list should not be null.", list );
        assertEquals( "file list should contain 6 items", 6, list.size() );

        for ( int i = 0; i < filenames.length; i++ )
        {
            assertTrue( "Filename '" + filenames[i] + "' should be in list.", list.contains( filenames[i] ) );
        }

        for ( int i = 0; i < dirnames.length; i++ )
        {
            assertTrue( "Directory '" + dirnames[i] + "' should be in list.", list.contains( dirnames[i] + "/" ) );
        }

        ///////////////////////////////////////////////////////////////////////////
        list = wagon.getFileList( "" );
        assertNotNull( "file list should not be null.", list );
        assertEquals( "file list should contain 1 items", 1, list.size() );

        ///////////////////////////////////////////////////////////////////////////
        list = wagon.getFileList( dirName + "/test-dir1" );
        assertNotNull( "file list should not be null.", list );
        assertEquals( "file list should contain 0 items", 0, list.size() );

        /////////////////////////////////////////////////////////////////////////////
        try
        {
            list = wagon.getFileList( dirName + "/test-dir-bogus" );
            fail( "Exception expected" );
        }
        catch ( ResourceDoesNotExistException e )
        {
        
        }
        
        wagon.disconnect();
        

        tearDownWagonTestingFixtures();
    }
}
