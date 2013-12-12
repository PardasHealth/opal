/*
 * Copyright (c) 2013 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.opal.web.security;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.bouncycastle.openssl.PEMWriter;
import org.obiba.opal.core.security.OpalKeyStore;
import org.obiba.opal.core.service.security.KeyStoreService;
import org.obiba.opal.web.magma.ClientErrorDtos;
import org.obiba.opal.web.model.Opal;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import static javax.ws.rs.core.Response.ResponseBuilder;
import static javax.ws.rs.core.Response.Status;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class KeyStoreResource {

  private OpalKeyStore keyStore;

  private KeyStoreService keyStoreService;

  @GET
  public List<Opal.KeyDto> getKeyEntries() throws KeyStoreException, IOException {
    List<Opal.KeyDto> keyEntries = Lists.newArrayList();
    if(keyStore != null) {
      for(String alias : keyStore.listAliases()) {
        Opal.KeyType type = Opal.KeyType.valueOf(keyStore.getKeyType(alias).toString());
        Opal.KeyDto.Builder kpBuilder = Opal.KeyDto.newBuilder().setAlias(alias).setKeyType(type);

        kpBuilder.setCertificate(getPEMCertificate(keyStore, alias));
        keyEntries.add(kpBuilder.build());
      }

      sortByName(keyEntries);
    }
    return keyEntries;
  }

  @POST
  public Response createKeyEntry(Opal.KeyForm keyForm) {

    if(keyStore.aliasExists(keyForm.getAlias())) {
      return Response.status(Status.BAD_REQUEST)
          .entity(ClientErrorDtos.getErrorMessage(Status.BAD_REQUEST, "KeyEntryAlreadyExists").build()).build();
    }

    ResponseBuilder responseBuilder = keyForm.getKeyType() == Opal.KeyType.KEY_PAIR
        ? doCreateOrImportKeyPair(keyForm)
        : doImportCertificate(keyForm);
    if(responseBuilder == null) {
      keyStoreService.saveKeyStore(keyStore);
      responseBuilder = Response.created(URI.create(""));
    }

    return responseBuilder.build();
  }

  @PUT
  public Response updateKeyEntry(Opal.KeyForm keyForm) {

    ResponseBuilder responseBuilder = keyForm.getKeyType() == Opal.KeyType.KEY_PAIR
        ? doCreateOrImportKeyPair(keyForm)
        : doImportCertificate(keyForm);
    if(responseBuilder == null) {
      keyStoreService.saveKeyStore(keyStore);
      responseBuilder = Response.ok();
    }

    return responseBuilder.build();
  }

  @GET
  @Path("/{alias}")
  public Response getKeyEntry(@PathParam("alias") String alias) throws IOException, KeyStoreException {
    if(!keyStore.aliasExists(alias)) return Response.status(Status.NOT_FOUND).build();

    Opal.KeyType type = Opal.KeyType.valueOf(keyStore.getKeyType(alias).toString());
    Opal.KeyDto.Builder keyBuilder = Opal.KeyDto.newBuilder().setAlias(alias).setKeyType(type);

    keyBuilder.setCertificate(getPEMCertificate(keyStore, alias));

    return Response.ok().entity(keyBuilder.build()).build();
  }

  @DELETE
  @Path("/{alias}")
  public Response deleteKeyEntry(@PathParam("alias") String alias) {
    if(!keyStore.aliasExists(alias)) {
      return Response.status(Status.NOT_FOUND).build();
    }

    keyStore.deleteKey(alias);
    return Response.ok().build();
  }

  @GET
  @Path("/{alias}/certificate")
  public Response getCertificate(@PathParam("alias") String alias) throws IOException, KeyStoreException {
    return Response.ok(getPEMCertificate(keyStore, alias), MediaType.TEXT_PLAIN_TYPE).header("Content-disposition",
        "attachment; filename=\"" + keyStore.getName() + "-" + alias + "-certificate.pem\"").build();
  }

  @Nullable
  private ResponseBuilder doImportCertificate(Opal.KeyForm keyForm) {

    keyStore.importCertificate(keyForm.getAlias(), new ByteArrayInputStream(keyForm.getPublicImport().getBytes()));
    return null;
  }

  @Nullable
  private ResponseBuilder doCreateOrImportKeyPair(Opal.KeyForm keyForm) {
    ResponseBuilder response = null;
    if(keyForm.hasPrivateForm() && keyForm.hasPublicForm()) {
      keyStore.createOrUpdateKey(keyForm.getAlias(), keyForm.getPrivateForm().getAlgo(), keyForm.getPrivateForm().getSize(),
              getCertificateInfo(keyForm.getPublicForm()));
    } else {
      response = keyForm.hasPrivateImport() ? doImportKeyPair(keyForm) : Response.status(Status.BAD_REQUEST)
              .entity(ClientErrorDtos.getErrorMessage(Status.BAD_REQUEST, "MissingPrivateKeyArgument").build());
    }
    return response;
  }

  @Nullable
  private ResponseBuilder doImportKeyPair(Opal.KeyForm keyForm) {
    ResponseBuilder response = null;
    if(keyForm.hasPublicForm()) {
      keyStore.importKey(keyForm.getAlias(), new ByteArrayInputStream(keyForm.getPrivateImport().getBytes()),
          getCertificateInfo(keyForm.getPublicForm()));
    } else if(keyForm.hasPublicImport()) {
      keyStore.importKey(keyForm.getAlias(), new ByteArrayInputStream(keyForm.getPrivateImport().getBytes()),
          new ByteArrayInputStream(keyForm.getPublicImport().getBytes()));
    } else {
      response = Response.status(Status.BAD_REQUEST)
          .entity(ClientErrorDtos.getErrorMessage(Status.BAD_REQUEST, "MissingPublicKeyArgument").build());
    }
    return response;
  }

  private String getCertificateInfo(Opal.PublicKeyForm pkForm) {
    return "CN=" + pkForm.getName() + ", OU=" + pkForm.getOrganizationalUnit() + ", O=" + pkForm.getOrganization() +
        ", L=" + pkForm.getLocality() + ", ST=" + pkForm.getState() + ", C=" + pkForm.getCountry();
  }

  private String getPEMCertificate(OpalKeyStore keystore, String alias) throws KeyStoreException, IOException {
    Certificate certificate = keystore.getKeyStore().getCertificate(alias);
    if(certificate == null) throw new IllegalArgumentException("Cannot find certificate for alias: " + alias);

    StringWriter writer = new StringWriter();
    PEMWriter pemWriter = new PEMWriter(writer);
    pemWriter.writeObject(certificate);
    pemWriter.flush();
    return writer.getBuffer().toString();
  }

  private void sortByName(List<Opal.KeyDto> units) {
    // sort alphabetically
    Collections.sort(units, new Comparator<Opal.KeyDto>() {

      @Override
      public int compare(Opal.KeyDto d1, Opal.KeyDto d2) {
        return d1.getAlias().compareTo(d2.getAlias());
      }

    });
  }

  public void setKeyStore(OpalKeyStore keyStore) {
    this.keyStore = keyStore;
  }

  public void setKeyStoreService(KeyStoreService keyStoreService) {
    this.keyStoreService = keyStoreService;
  }
}
