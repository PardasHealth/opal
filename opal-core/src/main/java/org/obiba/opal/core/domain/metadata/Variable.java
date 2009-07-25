/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.core.domain.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.obiba.core.domain.AbstractEntity;

/**
 *
 */
@javax.persistence.Entity
public class Variable extends AbstractEntity {

  private static final long serialVersionUID = 1L;

  @ManyToOne
  private Catalogue catalogue;

  @Column(nullable = false, length = 2000)
  private String name;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "variable")
  private List<VariableAttribute> attributes;

  public Variable() {

  }

  public Variable(Catalogue catalogue, String name) {
    this.catalogue = catalogue;
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public Catalogue getCatalogue() {
    return catalogue;
  }

  public List<VariableAttribute> getAttributes() {
    return attributes != null ? attributes : (attributes = new ArrayList<VariableAttribute>());
  }

  public VariableAttribute addAttribute(String name, Object value) {
    return this.addAttribute(name, value != null ? value.toString() : null);
  }

  public VariableAttribute addAttribute(String name, boolean value) {
    return this.addAttribute(name, Boolean.toString(value));
  }

  public VariableAttribute addAttribute(String name, String value) {
    VariableAttribute va = new VariableAttribute(this, name, value);
    getAttributes().add(va);
    return va;
  }

  public VariableAttribute addAttribute(String name, String value, Locale lc) {
    VariableAttribute va = new VariableAttribute(this, name, value, lc);
    getAttributes().add(va);
    return va;
  }
}
