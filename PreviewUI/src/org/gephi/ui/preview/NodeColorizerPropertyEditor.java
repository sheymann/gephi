package org.gephi.ui.preview;

import org.gephi.preview.api.color.Colorizer;
import org.gephi.preview.api.color.ColorizerFactory;
import org.gephi.preview.api.color.ColorizerType;
import org.openide.util.Lookup;

/**
 *
 * @author jeremy
 */
public class NodeColorizerPropertyEditor extends AbstractColorizerPropertyEditor {

    protected void setSupportedColorizerTypes() {
        addSupportedColorizerType(ColorizerType.CUSTOM);
        addSupportedColorizerType(ColorizerType.NODE_ORIGINAL);
    }

    protected Colorizer createColorizer(ColorizerType type) {
        ColorizerFactory f = Lookup.getDefault().lookup(ColorizerFactory.class);
        return f.createNodeColorizer(type);
    }
}
