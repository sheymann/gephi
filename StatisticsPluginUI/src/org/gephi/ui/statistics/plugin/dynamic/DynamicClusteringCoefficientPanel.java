/*
Copyright 2008-2010 Gephi
Authors : Mathieu Bastian
Website : http://www.gephi.org

This file is part of Gephi.

DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

Copyright 2011 Gephi Consortium. All rights reserved.

The contents of this file are subject to the terms of either the GNU
General Public License Version 3 only ("GPL") or the Common
Development and Distribution License("CDDL") (collectively, the
"License"). You may not use this file except in compliance with the
License. You can obtain a copy of the License at
http://gephi.org/about/legal/license-notice/
or /cddl-1.0.txt and /gpl-3.0.txt. See the License for the
specific language governing permissions and limitations under the
License.  When distributing the software, include this License Header
Notice in each file and include the License files at
/cddl-1.0.txt and /gpl-3.0.txt. If applicable, add the following below the
License Header, with the fields enclosed by brackets [] replaced by
your own identifying information:
"Portions Copyrighted [year] [name of copyright owner]"

If you wish your version of this file to be governed by only the CDDL
or only the GPL Version 3, indicate your decision by adding
"[Contributor] elects to include this software in this distribution
under the [CDDL or GPL Version 3] license." If you do not indicate a
single choice of license, a recipient has the option to distribute
your version of this file under either the CDDL, the GPL Version 3 or
to extend the choice of license to its licensees as provided above.
However, if you add GPL Version 3 code and therefore, elected the GPL
Version 3 license, then the option applies only if the new code is
made subject to such option by the copyright holder.

Contributor(s):

Portions Copyrighted 2011 Gephi Consortium.
 */
package org.gephi.ui.statistics.plugin.dynamic;

import org.gephi.graph.api.GraphController;
import org.openide.util.Lookup;

/**
 *
 * @author Mathieu Bastian
 */
public class DynamicClusteringCoefficientPanel extends javax.swing.JPanel {

    /** Creates new form DynamicClusteringCoefficientPanel */
    public DynamicClusteringCoefficientPanel() {
        initComponents();

        //Disable directed if the graph is undirecteds
        GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
        if (graphController.getModel().isUndirected()) {
            directedRadioButton.setEnabled(false);
        }
    }

    public boolean isDirected() {
        return directedRadioButton.isSelected();
    }

    public void setDirected(boolean directed) {
        directedButtonGroup.setSelected(directed ? directedRadioButton.getModel() : undirectedRadioButton.getModel(), true);
        if (!directed) {
            directedRadioButton.setEnabled(false);
        }
    }

    public boolean isAverageOnly() {
        return averageOnlyCheckbox.isSelected();
    }

    public void setAverageOnly(boolean averageOnly) {
        averageOnlyCheckbox.setSelected(averageOnly);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        directedButtonGroup = new javax.swing.ButtonGroup();
        header = new org.jdesktop.swingx.JXHeader();
        directedRadioButton = new javax.swing.JRadioButton();
        undirectedRadioButton = new javax.swing.JRadioButton();
        averageOnlyCheckbox = new javax.swing.JCheckBox();

        header.setDescription(org.openide.util.NbBundle.getMessage(DynamicClusteringCoefficientPanel.class, "DynamicClusteringCoefficientPanel.header.description")); // NOI18N
        header.setTitle(org.openide.util.NbBundle.getMessage(DynamicClusteringCoefficientPanel.class, "DynamicClusteringCoefficientPanel.header.title")); // NOI18N

        directedButtonGroup.add(directedRadioButton);
        directedRadioButton.setText(org.openide.util.NbBundle.getMessage(DynamicClusteringCoefficientPanel.class, "DynamicClusteringCoefficientPanel.directedRadioButton.text")); // NOI18N

        directedButtonGroup.add(undirectedRadioButton);
        undirectedRadioButton.setText(org.openide.util.NbBundle.getMessage(DynamicClusteringCoefficientPanel.class, "DynamicClusteringCoefficientPanel.undirectedRadioButton.text")); // NOI18N

        averageOnlyCheckbox.setText(org.openide.util.NbBundle.getMessage(DynamicClusteringCoefficientPanel.class, "DynamicClusteringCoefficientPanel.averageOnlyCheckbox.text")); // NOI18N

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(undirectedRadioButton)
                    .add(directedRadioButton)
                    .add(averageOnlyCheckbox))
                .addContainerGap(241, Short.MAX_VALUE))
            .add(header, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(header, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 93, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(directedRadioButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(undirectedRadioButton)
                .add(18, 18, 18)
                .add(averageOnlyCheckbox)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox averageOnlyCheckbox;
    private javax.swing.ButtonGroup directedButtonGroup;
    protected javax.swing.JRadioButton directedRadioButton;
    private org.jdesktop.swingx.JXHeader header;
    protected javax.swing.JRadioButton undirectedRadioButton;
    // End of variables declaration//GEN-END:variables
}
