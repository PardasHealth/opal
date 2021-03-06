/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.opal.web.magma;

import java.util.Locale;
import java.util.Set;

import javax.ws.rs.PUT;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.obiba.opal.web.model.Magma;

import javax.annotation.Nullable;

public interface VariableViewResource extends VariableResource {

  void setLocales(Set<Locale> locales);

  @PUT
  Response createOrUpdateVariable(Magma.VariableDto variable, @Nullable @QueryParam("comment") String comment);
}
