/**
 * Copyright (c) 2014, Sindice Limited. All Rights Reserved.
 *
 * This file is part of the SIREn project.
 *
 * SIREn is a free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * SIREn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.sindicetech.siren.search.node;

import java.io.IOException;
import java.util.Collection;

import org.apache.lucene.search.Weight;
import org.apache.lucene.util.IntsRef;

import com.sindicetech.siren.index.DocsAndNodesIterator;
import com.sindicetech.siren.util.NodeUtils;

/**
 * An extensions of {@link NodeConjunctionScorer} for conjunctions of an
 * ancestor {@link NodeScorer} and a set of descendant {@link NodeScorer}s.
 *
 * <p>
 *
 * The root node (ancestor node) is considered matching if its scorers and all
 * the descendant scorers match the same node.
 **/
class TwigConjunctionScorer extends NodeConjunctionScorer {

  private final NodeScorer root;
  private final NodeScorer[] descendants;

  public TwigConjunctionScorer(final Weight weight, final float coord,
                               final NodeScorer root,
                               final Collection<NodeScorer> scorers)
  throws IOException {
    this(weight, coord, root, scorers.toArray(new NodeScorer[scorers.size()]));
  }

  public TwigConjunctionScorer(final Weight weight, final float coord,
                               final NodeScorer root,
                               final NodeScorer ... scorers)
  throws IOException {
    super(weight, coord, append(scorers, root));
    this.root = root;
    this.descendants = scorers;
  }

  private static final NodeScorer[] append(final NodeScorer[] array, final NodeScorer element) {
    final NodeScorer[] newArray = new NodeScorer[array.length + 1];
    System.arraycopy(array, 0, newArray, 0, array.length);
    newArray[newArray.length - 1] = element;
    return newArray;
  }

  @Override
  public int doc() {
    // return root.doc();
    return lastDocument;
  }

  @Override
  public IntsRef node() {
    // return root.node();
    return lastNode;
  }

  @Override
  public boolean nextNode() throws IOException {
  root: // label statement for the beginning of the loop
    while (root.nextNode()) {
      for (int i = 0; i < descendants.length; i++) {
        int c;
        while ((c = NodeUtils.compareAncestor(root.node(), descendants[i].node())) > 0) {
          if (!descendants[i].nextNode()) {
            lastNode = DocsAndNodesIterator.NO_MORE_NOD;
            return false;
          }
        }
        if (c < 0) { // root node behind
          // continue to the label statement and move to the next root's node
          continue root;
        }
      }
      // all equals
      lastNode = root.node();
      return true;
    }
    lastNode = DocsAndNodesIterator.NO_MORE_NOD;
    return false;
  }

  @Override
  public String toString() {
    return "TwigConjunctionScorer(" + weight + "," + this.doc() + "," +
      this.node() + ")";
  }

}
