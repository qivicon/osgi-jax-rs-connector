/*******************************************************************************
 * Copyright (c) 2014,2015 EclipseSource and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: Holger Staudacher - initial API and
 * implementation Ivan Iliev - Performance Optimizations
 ******************************************************************************/
package com.eclipsesource.jaxrs.publisher.internal;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.omg.CORBA.Request;

public class ServletContainerBridge extends HttpServlet implements Runnable {

  private final RootApplication application;
  private ServletContainer servletContainer;
  private ServletConfig servletConfig;
  private volatile boolean isJerseyReady;

  public ServletContainerBridge( RootApplication application ) {
    this.servletContainer = new ServletContainer( ResourceConfig.forApplication( application ) );
    this.application = application;
    this.isJerseyReady = false;
  }

  @Override
  public void run() {
    if( application.isDirty() ) {
      ClassLoader original = Thread.currentThread().getContextClassLoader();
      try {
        Thread.currentThread().setContextClassLoader( Request.class.getClassLoader() );
        synchronized( this ) {
          isJerseyReady = false;
          // No WebComponent present, initialize Jersey so it's created
          if( getServletContainer().getWebComponent() == null ) {
            getServletContainer().init( servletConfig );
          }
          // We already have a WebComponent we need to reload it
          else {
            getServletContainer().destroy();
            // create a new ServletContainer when the old one is destroyed.
            this.servletContainer = new ServletContainer( ResourceConfig
              .forApplication( application ) );
            getServletContainer().init( servletConfig );
          }
          isJerseyReady = true;
        }
      } catch( Throwable e ) {
        e.printStackTrace();
        throw new RuntimeException( e );
      } finally {
        Thread.currentThread().setContextClassLoader( original );
      }
    }
  }

  @Override
  public void init( ServletConfig config ) throws ServletException {
    application.setDirty( true );
    this.servletConfig = config;
  }

  @Override
  public void service( ServletRequest req, ServletResponse res )
    throws ServletException, IOException
  {
    // if jersey has not yet been initialized return service unavailable
    if( isJerseyReady() ) {
      getServletContainer().service( req, res );
    } else {
      ( ( HttpServletResponse )res ).sendError( HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                                                "Jersey is not ready yet!" );
    }
  }

  @Override
  public void destroy() {
    synchronized( this ) {
      if( isJerseyReady() ) {
        this.isJerseyReady = false;
        getServletContainer().destroy();
        // create a new ServletContainer when the old one is destroyed.
        this.servletContainer = new ServletContainer( ResourceConfig
          .forApplication( application ) );
      }
    }
  }

  // for testing purposes
  ServletContainer getServletContainer() {
    return servletContainer;
  }

  void setJerseyReady( boolean isJerseyReady ) {
    this.isJerseyReady = isJerseyReady;
  }

  // for testing purposes
  boolean isJerseyReady() {
    return isJerseyReady;
  }
}
