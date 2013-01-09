package org.obiba.opal.fs.security;

import java.io.File;

import org.apache.commons.vfs2.FileObject;
import org.obiba.magma.security.Authorizer;
import org.obiba.magma.security.shiro.ShiroAuthorizer;
import org.obiba.opal.fs.OpalFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class SecuredOpalFileSystem implements OpalFileSystem {

  private static final Logger log = LoggerFactory.getLogger(OpalFileSystem.class);

  private final OpalFileSystem delegate;

  private final Authorizer authorizer = new ShiroAuthorizer();

  public SecuredOpalFileSystem(OpalFileSystem delegate) {
    Preconditions.checkArgument(delegate != null);
    this.delegate = delegate;
  }

  @Override
  public FileObject getRoot() {
    return new SecuredFileObject(authorizer,delegate.getRoot());
  }

  @Override
  public File getLocalFile(FileObject virtualFile) {
    return delegate.getLocalFile(virtualFile);
  }

  @Override
  public File convertVirtualFileToLocal(FileObject virtualFile) {
    return delegate.convertVirtualFileToLocal(virtualFile);
  }

  @Override
  public boolean isLocalFile(FileObject virtualFile) {
    return delegate.isLocalFile(virtualFile);
  }

  @Override
  public String getObfuscatedPath(FileObject virtualFile) {
    return delegate.getObfuscatedPath(virtualFile);
  }

  @Override
  public FileObject resolveFileFromObfuscatedPath(FileObject baseFolder, String obfuscatedPath) {
    return new SecuredFileObject(authorizer,delegate.resolveFileFromObfuscatedPath(baseFolder, obfuscatedPath));
  }
}
