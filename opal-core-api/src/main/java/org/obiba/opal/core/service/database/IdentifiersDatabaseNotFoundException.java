/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.opal.core.service.database;

public class IdentifiersDatabaseNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 2416927981990092192L;

  public IdentifiersDatabaseNotFoundException() {
    super("Database for identifiers is not defined");
  }
}
