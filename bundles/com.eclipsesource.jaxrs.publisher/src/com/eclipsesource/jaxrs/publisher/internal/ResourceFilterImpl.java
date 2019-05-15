package com.eclipsesource.jaxrs.publisher.internal;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

import com.eclipsesource.jaxrs.publisher.ResourceFilter;

/**
 */
public class ResourceFilterImpl implements ResourceFilter {

  private Filter filter;

  public ResourceFilterImpl( String filterString ) throws InvalidSyntaxException {
    filter = FrameworkUtil.createFilter( filterString );
  }

  @Override
  public Filter getFilter() {
    return filter;
  }
}
